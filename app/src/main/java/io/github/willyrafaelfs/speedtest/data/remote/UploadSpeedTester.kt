package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink

/**
 * Mengukur kecepatan upload lewat [SpeedTestConfig.streamCount] koneksi paralel yang
 * masing-masing mengirim request POST berukuran tetap secara berulang ke
 * [SpeedTestConfig.uploadUrl].
 *
 * Byte yang ditulis ke socket belum tentu sudah sampai di server: kernel menampungnya
 * dulu di send buffer, sehingga di awal angka bisa terlihat terlalu tinggi. Karena itu:
 * - **tampilan live** memakai byte yang ditulis (agar gauge responsif), sedangkan
 * - **hasil final** hanya menghitung request yang sudah dijawab server. Byte tiap request
 *   dianggap tersebar merata dari request dimulai sampai response diterima.
 *
 * Ukuran request mulai dari [SpeedTestConfig.uploadMinRequestBytes] dan digandakan selama
 * request selesai lebih cepat dari [SpeedTestConfig.uploadTargetRequestMillis], supaya
 * koneksi lambat tetap menyelesaikan request dan koneksi cepat tidak boros overhead per request.
 */
class UploadSpeedTester @Inject constructor(
    private val client: OkHttpClient,
    private val config: SpeedTestConfig,
    private val pingTester: PingTester,
) {
    // Data acak agar tidak terkompresi (kompresi bisa memalsukan kecepatan).
    private val chunk = ByteArray(WRITE_CHUNK_BYTES).also { Random.nextBytes(it) }

    fun measure(): Flow<SpeedSample> = measureThroughput(config, pingTester, UploadWorkload())

    private inner class UploadWorkload : ThroughputWorkload {
        private val completed = ConcurrentLinkedQueue<ByteSpan>()

        // Waktu mulai request yang sedang berjalan per koneksi. Request yang belum selesai
        // saat fase dihentikan akan dibatalkan, jadi datanya tidak lengkap.
        private val inFlightStarts = ConcurrentHashMap<Int, Long>()

        override fun runStream(streamIndex: Int, run: PhaseRun) {
            val targetNanos = config.uploadTargetRequestMillis.millisToNanos()
            var size = config.uploadMinRequestBytes
            while (run.isActive()) {
                val request = Request.Builder()
                    .url(config.uploadUrl)
                    .post(RandomBody(size, run.liveBytes))
                    .build()

                val start = System.nanoTime()
                inFlightStarts[streamIndex] = start
                run.execute(client.newCall(request)) { it.requireSuccess() }
                val end = System.nanoTime()
                inFlightStarts.remove(streamIndex)

                completed += ByteSpan(start, end, size)
                if (end - start < targetNanos) size = (size * 2).coerceAtMost(config.uploadMaxRequestBytes)
            }
        }

        /**
         * Data lengkap hanya sampai request tertua yang belum selesai dimulai: setelah titik
         * itu, ada koneksi yang byte-nya tidak terhitung karena request-nya dibatalkan.
         */
        override fun confirmedBytes(run: PhaseRun): ConfirmedBytes {
            val validUntil = (inFlightStarts.values.minOrNull() ?: run.deadlineNanos).coerceAtMost(run.deadlineNanos)
            return ConfirmedBytes(completed.toList(), validUntil)
        }
    }

    /** Body berisi [size] byte acak yang ikut menambah [writtenBytes] saat ditulis ke socket. */
    private inner class RandomBody(private val size: Long, private val writtenBytes: AtomicLong) : RequestBody() {
        override fun contentType() = OCTET_STREAM

        override fun contentLength() = size

        override fun writeTo(sink: BufferedSink) {
            var remaining = size
            while (remaining > 0) {
                val count = minOf(remaining, chunk.size.toLong()).toInt()
                sink.write(chunk, 0, count)
                // flush agar byte benar-benar diserahkan ke socket sebelum dihitung.
                sink.flush()
                writtenBytes.addAndGet(count.toLong())
                remaining -= count
            }
        }
    }

    private companion object {
        const val WRITE_CHUNK_BYTES = 64 * 1024
        val OCTET_STREAM = "application/octet-stream".toMediaType()
    }
}
