package com.example.networkspeedtest.data.remote

import com.example.networkspeedtest.domain.model.SpeedSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.io.IOException
import javax.inject.Inject

/**
 * Mengukur kecepatan download dengan mengunduh data dari [SpeedTestConfig.downloadUrl]
 * dan menghitung byte yang diterima per satuan waktu.
 *
 * Memancarkan [SpeedSample] setiap [SpeedTestConfig.sampleIntervalMillis] agar UI
 * bisa menampilkan angka/gauge yang bergerak, lalu satu sampel final berisi rata-rata
 * keseluruhan.
 */
class DownloadSpeedTester @Inject constructor(
    private val client: OkHttpClient,
    private val config: SpeedTestConfig,
) {
    fun measure(): Flow<SpeedSample> = flow {
        val startNanos = System.nanoTime()
        val durationNanos = config.testDurationMillis * NANOS_PER_MILLI
        val sampleIntervalNanos = config.sampleIntervalMillis * NANOS_PER_MILLI
        val deadline = startNanos + durationNanos

        var totalBytes = 0L
        var lastSampleNanos = startNanos
        var lastSampleBytes = 0L
        val sink = Buffer()

        // Menghitung sampel kecepatan pada waktu `now`.
        // Mbps = bytes * 8 bit / detik / 1e6 (satuan desimal, standar kecepatan jaringan).
        fun sampleAt(now: Long): SpeedSample {
            val intervalSec = (now - lastSampleNanos).coerceAtLeast(1L) / NANOS_PER_SEC
            val elapsedSec = (now - startNanos).coerceAtLeast(1L) / NANOS_PER_SEC
            val instant = (totalBytes - lastSampleBytes) * BITS_PER_BYTE / intervalSec / BITS_PER_MBIT
            val average = totalBytes * BITS_PER_BYTE / elapsedSec / BITS_PER_MBIT
            val progress = ((now - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
            return SpeedSample(instantMbps = instant, averageMbps = average, progressFraction = progress)
        }

        // Ulangi request hingga batas waktu tercapai — menangani koneksi cepat yang
        // menghabiskan satu file sebelum durasi test berakhir.
        while (System.nanoTime() < deadline) {
            val request = Request.Builder().url(config.downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download gagal: HTTP ${response.code}")
                val source = response.body?.source() ?: throw IOException("Response body kosong")

                while (System.nanoTime() < deadline) {
                    val read = source.read(sink, READ_CHUNK_BYTES)
                    if (read == -1L) return@use // file habis, minta lagi di iterasi luar
                    totalBytes += read
                    sink.clear() // buang data; kita hanya butuh jumlah byte-nya

                    val now = System.nanoTime()
                    if (now - lastSampleNanos >= sampleIntervalNanos) {
                        emit(sampleAt(now))
                        lastSampleNanos = now
                        lastSampleBytes = totalBytes
                    }
                }
            }
        }

        // Sampel final: progress penuh, kecepatan = rata-rata keseluruhan.
        val elapsedSec = (System.nanoTime() - startNanos).coerceAtLeast(1L) / NANOS_PER_SEC
        val average = totalBytes * BITS_PER_BYTE / elapsedSec / BITS_PER_MBIT
        emit(SpeedSample(instantMbps = average, averageMbps = average, progressFraction = 1f))
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val READ_CHUNK_BYTES = 64L * 1024
        const val NANOS_PER_MILLI = 1_000_000L
        const val NANOS_PER_SEC = 1_000_000_000.0
        const val BITS_PER_BYTE = 8.0
        const val BITS_PER_MBIT = 1_000_000.0
    }
}
