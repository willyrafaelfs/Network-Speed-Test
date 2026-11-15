package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.PingResult
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
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
    client: OkHttpClient,
    private val config: SpeedTestConfig,
) {
    private val request = Request.Builder().url(config.pingUrl).build()

    // Connection pool sendiri: koneksi ping yang sudah "hangat" tidak boleh diambil worker
    // download/upload. Tanpa ini, ping saat beban harus membuka koneksi TCP + TLS baru di
    // tengah link yang penuh, dan pada bufferbloat parah handshake itu bisa lebih lama dari
    // seluruh fase sehingga latency saat beban tidak terukur sama sekali.
    private val client = client.newBuilder().connectionPool(ConnectionPool()).build()

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

        summarize(latencies)
    }

    /**
     * Memastikan koneksi ping sudah terbuka (TCP + TLS) sebelum beban dimulai, sehingga
     * ping saat beban hanya mengukur round-trip, bukan handshake. Kegagalan diabaikan:
     * [sampleUnderLoad] tetap mencoba.
     */
    internal fun warmUp() {
        try {
            rttMillis()
        } catch (ignored: IOException) {
            // Akan terlihat sebagai ping yang gagal/lambat saat beban.
        }
    }

    /**
     * Ping berkala selama fase download/upload berjalan (latency saat beban), sampai
     * [run] dihentikan. Hasil ditambahkan ke [sink]; ping yang gagal diabaikan.
     *
     * Bila sampai fase berakhir belum ada ping yang selesai, lama ping yang sedang
     * berjalan dicatat sebagai **batas bawah** latency: kosong justru akan menyembunyikan
     * kondisi bufferbloat terparah.
     */
    internal fun sampleUnderLoad(run: PhaseRun, sink: MutableList<Double>) {
        while (run.isActive()) {
            val begin = System.nanoTime()
            try {
                sink += rttMillis(run)
            } catch (ignored: IOException) {
                // Ping di bawah beban berat wajar sesekali gagal; lanjutkan ke ping berikutnya.
                if (run.isStopping && sink.isEmpty()) sink += (System.nanoTime() - begin) / NANOS_PER_MILLI
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

    internal companion object {
        const val NANOS_PER_MILLI = 1_000_000.0

        /**
         * Latency = median sampel (tahan pencilan); jitter = rata-rata selisih absolut antar-latency
         * berurutan (mean deviation, RFC 3550).
         */
        internal fun summarize(latencies: List<Double>) =
            PingResult(latencyMs = ThroughputCalculator.percentile(latencies, MEDIAN), jitterMs = jitterOf(latencies))

        private const val MEDIAN = 0.5

        /** Jitter = rata-rata selisih absolut antar-latency berurutan (mean deviation, RFC 3550). */
        fun jitterOf(latencies: List<Double>): Double =
            if (latencies.size < 2) 0.0 else latencies.zipWithNext { a, b -> abs(b - a) }.average()
    }
}
