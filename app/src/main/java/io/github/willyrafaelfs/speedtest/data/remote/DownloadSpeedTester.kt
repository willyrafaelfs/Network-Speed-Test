package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.MeasurementOptions
import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer

/**
 * Mengukur kecepatan download lewat [SpeedTestConfig.streamCount] koneksi paralel
 * ke [SpeedTestConfig.downloadUrl], dengan menghitung byte yang diterima per satuan waktu.
 *
 * Beberapa koneksi dipakai karena satu koneksi TCP sering tidak sanggup mengisi link
 * cepat (dibatasi receive window dan pemulihan setelah packet loss). Hasil final
 * dihitung oleh [LiveMeter] setelah membuang fase ramp-up.
 */
class DownloadSpeedTester @Inject constructor(
    private val client: OkHttpClient,
    private val config: SpeedTestConfig,
    private val pingTester: PingTester,
) {
    private val request = Request.Builder()
        .url(config.downloadUrl)
        // Matikan gzip transparan OkHttp: byte yang dihitung harus byte yang lewat jaringan,
        // bukan hasil dekompresi.
        .header("Accept-Encoding", "identity")
        .build()

    /** @param options durasi & jumlah koneksi dari Settings; null = pakai [SpeedTestConfig] apa adanya. */
    fun measure(options: MeasurementOptions? = null): Flow<SpeedSample> =
        measureThroughput(options?.let(config::withOptions) ?: config, pingTester, DownloadWorkload())

    private inner class DownloadWorkload : ThroughputWorkload {
        override fun runStream(streamIndex: Int, run: PhaseRun) {
            val sink = Buffer()
            // Ulangi request hingga fase selesai: koneksi cepat bisa menghabiskan satu file
            // sebelum durasi test berakhir.
            while (run.isActive()) {
                run.execute(client.newCall(request)) { response ->
                    response.requireSuccess()
                    val source = response.body.source()
                    while (run.isActive()) {
                        val read = source.read(sink, READ_CHUNK_BYTES)
                        if (read == -1L) break // file habis, minta lagi di iterasi luar
                        run.liveBytes.addAndGet(read)
                        sink.clear() // buang data; kita hanya butuh jumlah byte-nya
                    }
                }
            }
        }
    }

    private companion object {
        const val READ_CHUNK_BYTES = 64L * 1024
    }
}
