package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.PingResult
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Mengukur latency & jitter dengan melakukan beberapa kali request HTTP ringan
 * ke [SpeedTestConfig.pingUrl] (endpoint 0 byte) dan mencatat waktu round-trip
 * tiap request.
 *
 * ICMP ping butuh akses root, maka RTT HTTP dipakai sebagai proxy latency yang wajar.
 */
class PingTester @Inject constructor(
    private val client: OkHttpClient,
    private val config: SpeedTestConfig,
) {
    private val request = Request.Builder().url(config.pingUrl).build()

    /** Latency & jitter saat koneksi idle. */
    suspend fun measure(): PingResult = withContext(Dispatchers.IO) {
        // Warm-up: buka koneksi + TLS handshake lebih dulu agar biaya sekali-pakai itu
        // tidak ikut terhitung. Bila request ini saja gagal, server memang tidak
        // terjangkau, jadi error-nya langsung diteruskan.
        rttMillis()

        val latencies = ArrayList<Double>(config.pingCount)
        var lastError: IOException? = null
        repeat(config.pingCount) {
            ensureActive() // hormati pembatalan di antara ping
            try {
                latencies += rttMillis()
            } catch (e: IOException) {
                // Satu ping gagal dihitung sebagai sampel hilang, bukan error fatal.
                lastError = e
            }
        }
        if (latencies.isEmpty()) throw lastError ?: IOException("Ping gagal: tidak ada respons dari server")

        PingResult(avgLatencyMs = latencies.average(), jitterMs = jitterOf(latencies))
    }

    /**
     * Ping berkala selama fase download/upload berjalan (latency saat beban), sampai
     * [run] dihentikan. Hasil ditambahkan ke [sink]; ping yang gagal diabaikan.
     *
     * Ping saat beban memakai koneksi terpisah dari koneksi throughput, sehingga
     * sampel pertama ikut menanggung TCP + TLS handshake dan sengaja dibuang.
     */
    internal fun sampleUnderLoad(run: PhaseRun, sink: MutableList<Double>) {
        var warmedUp = false
        while (run.isActive()) {
            val begin = System.nanoTime()
            try {
                val rtt = rttMillis(run)
                if (warmedUp) sink += rtt
                warmedUp = true
            } catch (ignored: IOException) {
                // Ping di bawah beban berat wajar sesekali gagal; lanjutkan ke ping berikutnya.
            }
            val elapsedMillis = (System.nanoTime() - begin) / NANOS_PER_MILLI
            run.awaitStop((config.loadedPingIntervalMillis - elapsedMillis.toLong()).coerceAtLeast(0))
        }
    }

    /** Waktu round-trip satu request dalam ms. */
    private fun rttMillis(run: PhaseRun? = null): Double {
        val call = client.newCall(request)
        val start = System.nanoTime()
        val consume: (Response) -> Double = { response ->
            response.requireSuccess()
            response.body.bytes() // konsumsi body sampai habis → RTT lengkap
            (System.nanoTime() - start) / NANOS_PER_MILLI
        }
        return run?.execute(call, consume) ?: call.execute().use(consume)
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000.0

        /** Jitter = rata-rata selisih absolut antar-latency berurutan (mean deviation, RFC 3550). */
        fun jitterOf(latencies: List<Double>): Double =
            if (latencies.size < 2) 0.0 else latencies.zipWithNext { a, b -> abs(b - a) }.average()
    }
}
