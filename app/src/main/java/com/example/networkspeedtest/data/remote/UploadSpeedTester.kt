package com.example.networkspeedtest.data.remote

import com.example.networkspeedtest.domain.model.SpeedSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random

/**
 * Mengukur kecepatan upload dengan mengirim data acak ke [SpeedTestConfig.uploadUrl]
 * secara terus-menerus selama durasi test, sambil menghitung byte yang benar-benar
 * ditulis ke socket.
 *
 * Byte ditulis dari dalam [RequestBody.writeTo] (blocking), maka pengukuran memakai
 * [callbackFlow]: request dijalankan di Dispatchers.IO, dan sampel dikirim ke Flow
 * lewat channel.
 */
class UploadSpeedTester @Inject constructor(
    private val client: OkHttpClient,
    private val config: SpeedTestConfig,
) {
    fun measure(): Flow<SpeedSample> = callbackFlow {
        val startNanos = System.nanoTime()
        val durationNanos = config.testDurationMillis * NANOS_PER_MILLI
        val sampleIntervalNanos = config.sampleIntervalMillis * NANOS_PER_MILLI
        val deadline = startNanos + durationNanos

        // Data acak agar tidak terkompresi (kompresi bisa memalsukan kecepatan).
        val chunk = ByteArray(UPLOAD_CHUNK_BYTES).also { Random.nextBytes(it) }

        // State pengukuran; ditulis di writeTo lalu dibaca setelah request selesai.
        // Aman tanpa sinkronisasi karena keduanya berjalan berurutan di coroutine IO yang sama.
        var totalBytes = 0L

        val body = object : RequestBody() {
            override fun contentType() = OCTET_STREAM

            override fun writeTo(sink: BufferedSink) {
                var lastSampleNanos = startNanos
                var lastSampleBytes = 0L
                while (System.nanoTime() < deadline) {
                    sink.write(chunk)
                    totalBytes += chunk.size

                    val now = System.nanoTime()
                    if (now - lastSampleNanos >= sampleIntervalNanos) {
                        sink.flush() // paksa terkirim agar byte terhitung = byte di jaringan
                        val intervalSec = (now - lastSampleNanos).coerceAtLeast(1L) / NANOS_PER_SEC
                        val elapsedSec = (now - startNanos).coerceAtLeast(1L) / NANOS_PER_SEC
                        val instant = (totalBytes - lastSampleBytes) * BITS_PER_BYTE / intervalSec / BITS_PER_MBIT
                        val average = totalBytes * BITS_PER_BYTE / elapsedSec / BITS_PER_MBIT
                        val progress = ((now - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                        trySend(SpeedSample(instant, average, progress))
                        lastSampleNanos = now
                        lastSampleBytes = totalBytes
                    }
                }
                sink.flush()
            }
        }

        val call = client.newCall(Request.Builder().url(config.uploadUrl).post(body).build())

        // execute() blocking → jalankan di IO agar tidak memblokir collector.
        val job = launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Upload gagal: HTTP ${response.code}")
                }
                // Sampel final: rata-rata keseluruhan.
                val elapsedSec = (System.nanoTime() - startNanos).coerceAtLeast(1L) / NANOS_PER_SEC
                val average = totalBytes * BITS_PER_BYTE / elapsedSec / BITS_PER_MBIT
                trySend(SpeedSample(instantMbps = average, averageMbps = average, progressFraction = 1f))
                close()
            } catch (e: Exception) {
                close(e) // teruskan error ke collector
            }
        }

        awaitClose {
            call.cancel()
            job.cancel()
        }
    }

    private companion object {
        const val UPLOAD_CHUNK_BYTES = 64 * 1024
        val OCTET_STREAM = "application/octet-stream".toMediaType()
        const val NANOS_PER_MILLI = 1_000_000L
        const val NANOS_PER_SEC = 1_000_000_000.0
        const val BITS_PER_BYTE = 8.0
        const val BITS_PER_MBIT = 1_000_000.0
    }
}
