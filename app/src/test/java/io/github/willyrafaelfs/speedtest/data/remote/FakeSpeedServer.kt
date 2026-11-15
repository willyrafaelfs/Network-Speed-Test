package io.github.willyrafaelfs.speedtest.data.remote

import java.io.Closeable
import java.util.Collections
import java.util.concurrent.TimeUnit
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer

/**
 * Server HTTP lokal yang meniru endpoint speed test (`/down`, `/up`, `/ping`) dengan
 * bandwidth yang bisa dibatasi, sehingga hasil engine bisa dibandingkan dengan laju
 * yang diketahui.
 *
 * Throttle MockWebServer bekerja dengan tidur selama [throttlePeriodMillis] setiap kali
 * [throttleBytes] byte terkirim/diterima, per koneksi. Laju nominal per koneksi =
 * [bytesPerSecondPerConnection]; laju aktual sedikit lebih tinggi karena tidak ada jeda
 * setelah gumpalan terakhir tiap body.
 */
class FakeSpeedServer(
    private val throttleBytes: Long = 32L * 1024,
    private val throttlePeriodMillis: Long = 50,
    private val downloadBodyBytes: Int = 512 * 1024,
    private val pingDelayMillis: Long = 0,
) : Closeable {

    /** Status HTTP yang dikembalikan per path; default 200. */
    val statusOverrides: MutableMap<String, Int> = Collections.synchronizedMap(mutableMapOf())

    /** Semua request yang diterima, untuk diperiksa test. */
    val requests: MutableList<RecordedRequest> = Collections.synchronizedList(mutableListOf())

    val bytesPerSecondPerConnection: Double get() = throttleBytes * MILLIS_PER_SEC / throttlePeriodMillis.toDouble()

    private val downloadBody = ByteArray(downloadBodyBytes)

    private val server = MockWebServer().apply {
        // Jangan simpan body upload di memori; ukurannya tetap tercatat di bodySize.
        bodyLimit = 0
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.url.encodedPath
                statusOverrides[path]?.let { return MockResponse.Builder().code(it).build() }
                return when (path) {
                    "/down" -> MockResponse.Builder()
                        .body(Buffer().write(downloadBody))
                        .throttleBody(throttleBytes, throttlePeriodMillis, TimeUnit.MILLISECONDS)
                        .build()

                    "/ping" -> MockResponse.Builder()
                        .headersDelay(pingDelayMillis, TimeUnit.MILLISECONDS)
                        .build()

                    else -> MockResponse.Builder().build()
                }
            }

            // Kebijakan saat MEMBACA request body (upload) diambil dari peek().
            override fun peek(): MockResponse = MockResponse.Builder()
                .throttleBody(throttleBytes, throttlePeriodMillis, TimeUnit.MILLISECONDS)
                .build()
        }
        start()
    }

    fun url(path: String): String = server.url(path).toString()

    fun requestsTo(path: String): List<RecordedRequest> =
        synchronized(requests) { requests.filter { it.url.encodedPath == path } }

    /** Konfigurasi engine pendek (2 detik per fase) yang mengarah ke server ini. */
    fun config(
        testDurationMillis: Long = 2_000,
        streamCount: Int = 2,
    ) = SpeedTestConfig(
        downloadUrl = url("/down"),
        uploadUrl = url("/up"),
        pingUrl = url("/ping"),
        publicIpUrl = url("/ip"),
        testDurationMillis = testDurationMillis,
        sampleIntervalMillis = 100,
        pingCount = 5,
        streamCount = streamCount,
        rampUpMillis = 500,
        // Jendela 1 detik memuat ~20 gumpalan throttle, sehingga efek kuantisasi throttle
        // MockWebServer pada persentil tetap kecil (±5%).
        windowMillis = 1_000,
        loadedPingIntervalMillis = 100,
        uploadMinRequestBytes = 16L * 1024,
        uploadMaxRequestBytes = 256L * 1024,
        uploadTargetRequestMillis = 200,
    )

    override fun close() = server.close()

    private companion object {
        const val MILLIS_PER_SEC = 1_000.0
    }
}
