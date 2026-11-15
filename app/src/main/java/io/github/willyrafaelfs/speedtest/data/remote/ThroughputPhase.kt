package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Response

/**
 * State bersama satu fase download/upload yang sedang berjalan.
 *
 * Request OkHttp bersifat blocking dan tidak berhenti hanya karena coroutine-nya
 * dibatalkan, jadi semua [Call] aktif dicatat di sini agar bisa dihentikan paksa
 * lewat [stop] saat batas waktu tercapai atau saat user membatalkan test.
 */
class PhaseRun(val startNanos: Long, val deadlineNanos: Long) {
    /** Byte yang sudah diterima (download) atau ditulis ke socket (upload), untuk tampilan live. */
    val liveBytes = AtomicLong()

    private val activeCalls: MutableSet<Call> = ConcurrentHashMap.newKeySet()
    private val stopped = CountDownLatch(1)

    @Volatile
    var isStopping: Boolean = false
        private set

    /** True selama fase belum dihentikan dan batas waktu belum lewat. */
    fun isActive(): Boolean = !isStopping && System.nanoTime() < deadlineNanos

    /** Menjalankan [call] secara blocking dan memastikan ia ikut dihentikan oleh [stop]. */
    fun <T> execute(call: Call, block: (Response) -> T): T {
        activeCalls += call
        // Cegah race: call yang terdaftar tepat setelah stop() harus tetap dibatalkan.
        if (isStopping) call.cancel()
        try {
            return call.execute().use(block)
        } finally {
            activeCalls -= call
        }
    }

    /** Menunggu hingga [millis] ms, atau kembali lebih cepat bila fase dihentikan. */
    fun awaitStop(millis: Long) {
        stopped.await(millis, TimeUnit.MILLISECONDS)
    }

    /** Menghentikan fase: membatalkan seluruh request aktif agar thread IO segera bebas. */
    fun stop() {
        isStopping = true
        stopped.countDown()
        activeCalls.forEach(Call::cancel)
    }
}

/** Byte yang sudah dikonfirmasi server, beserta batas waktu sampai kapan datanya lengkap. */
data class ConfirmedBytes(val spans: List<ByteSpan>, val validUntilNanos: Long)

/** Pekerjaan spesifik download atau upload yang dijalankan di setiap koneksi paralel. */
interface ThroughputWorkload {
    /**
     * Dijalankan secara blocking di thread IO, satu kali per koneksi, dan harus kembali
     * saat [PhaseRun.isActive] bernilai false. IOException setelah fase dihentikan
     * dianggap wajar (akibat request dibatalkan) dan diabaikan oleh pemanggil.
     */
    fun runStream(streamIndex: Int, run: PhaseRun)

    /**
     * Data untuk hasil final bila berbeda dari [PhaseRun.liveBytes]. Default null:
     * hasil final dihitung dari byte live (tepat untuk download, karena byte yang
     * diterima memang sudah sampai di perangkat).
     */
    fun confirmedBytes(run: PhaseRun): ConfirmedBytes? = null
}

/**
 * Menjalankan satu fase throughput: [SpeedTestConfig.streamCount] koneksi paralel,
 * ping berkala untuk latency saat beban, dan sampel live setiap
 * [SpeedTestConfig.sampleIntervalMillis]. Sampel terakhir (progress 1f) berisi hasil final.
 */
internal fun measureThroughput(
    config: SpeedTestConfig,
    pingTester: PingTester,
    workload: ThroughputWorkload,
): Flow<SpeedSample> = channelFlow {
    pingTester.warmUp()
    val start = System.nanoTime()
    val run = PhaseRun(startNanos = start, deadlineNanos = start + config.testDurationMillis.millisToNanos())
    val meter = LiveMeter(start, config)
    val loadedLatencies: MutableList<Double> = Collections.synchronizedList(mutableListOf())

    try {
        val jobs = List(config.streamCount) { index ->
            launch(Dispatchers.IO) {
                try {
                    workload.runStream(index, run)
                } catch (e: IOException) {
                    // Request yang sengaja dibatalkan di akhir fase bukan error.
                    if (!run.isStopping) throw e
                }
            }
        } + launch(Dispatchers.IO) { pingTester.sampleUnderLoad(run, loadedLatencies) }

        while (true) {
            val remainingNanos = run.deadlineNanos - System.nanoTime()
            if (remainingNanos <= 0) break
            delay(minOf(config.sampleIntervalMillis, remainingNanos.nanosToMillisCeil()))
            send(meter.tick(System.nanoTime(), run.liveBytes.get(), loadedLatencies.medianOrNull()))
        }

        run.stop()
        jobs.joinAll()

        val final = workload.confirmedBytes(run)?.let { meter.finalMbps(it.spans, it.validUntilNanos) }
            ?: meter.finalMbps()
        send(SpeedSample(final, final, progressFraction = 1f, loadedLatencyMs = loadedLatencies.medianOrNull()))
    } finally {
        // Error di salah satu koneksi atau pembatalan oleh user: hentikan semua request.
        run.stop()
    }
}.flowOn(Dispatchers.IO)

/**
 * Mengubah total byte live menjadi [SpeedSample] di setiap tick, sekaligus
 * menyimpan riwayat byte per interval untuk perhitungan hasil final.
 */
internal class LiveMeter(private val startNanos: Long, config: SpeedTestConfig) {
    private val spans = mutableListOf<ByteSpan>()
    private var lastNanos = startNanos
    private var lastBytes = 0L

    private val windowNanos = config.windowMillis.millisToNanos()
    private val analysis = ThroughputCalculator.Analysis(
        windowNanos = windowNanos,
        stepNanos = config.sampleIntervalMillis.millisToNanos(),
        percentile = config.resultPercentile,
    )
    private val rampUpEndNanos = startNanos + config.rampUpMillis.millisToNanos()
    private val durationNanos = config.testDurationMillis.millisToNanos()

    fun tick(nowNanos: Long, totalBytes: Long, loadedLatencyMs: Double?): SpeedSample {
        spans += ByteSpan(lastNanos, nowNanos, totalBytes - lastBytes)
        lastNanos = nowNanos
        lastBytes = totalBytes

        // Kecepatan sesaat = rata-rata satu jendela terakhir, agar gauge tidak terlalu melompat.
        val instantFrom = maxOf(startNanos, nowNanos - windowNanos)
        val instant = ThroughputCalculator.mbps(
            ThroughputCalculator.bytesBetween(spans, instantFrom, nowNanos),
            nowNanos - instantFrom,
        )
        // Estimasi hasil memakai metode yang sama dengan hasil final, supaya angka tidak
        // melompat saat fase selesai. Selama ramp-up belum ada data yang dihitung.
        val estimate = if (nowNanos > rampUpEndNanos) finalMbps(spans, nowNanos) else instant
        val progress = ((nowNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
        return SpeedSample(instant, estimate, progress, loadedLatencyMs)
    }

    /** Hasil final dari [source] (default: byte live) hingga [untilNanos]. */
    fun finalMbps(source: List<ByteSpan> = spans, untilNanos: Long = lastNanos): Double =
        ThroughputCalculator.finalMbps(source, rampUpEndNanos, untilNanos, analysis) ?: fallbackMbps()

    // Rentang analisis kosong (mis. upload sangat lambat sehingga belum ada request yang
    // selesai setelah ramp-up): pakai rata-rata byte live sepanjang fase.
    private fun fallbackMbps(): Double = ThroughputCalculator.mbps(lastBytes.toDouble(), lastNanos - startNanos)
}

internal fun List<Double>.medianOrNull(): Double? {
    val snapshot = synchronized(this) { toList() }
    return if (snapshot.isEmpty()) null else ThroughputCalculator.percentile(snapshot, MEDIAN)
}

internal fun Long.millisToNanos(): Long = this * NANOS_PER_MILLI

private fun Long.nanosToMillisCeil(): Long = (this + NANOS_PER_MILLI - 1) / NANOS_PER_MILLI

private const val NANOS_PER_MILLI = 1_000_000L
private const val MEDIAN = 0.5
