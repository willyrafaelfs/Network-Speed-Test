package io.github.willyrafaelfs.speedtest.data.remote

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Sejumlah [bytes] yang berpindah di rentang waktu [startNanos]..[endNanos].
 * Byte dianggap tersebar merata di sepanjang rentang tersebut.
 *
 * Download memakai satu span per interval sampel (byte yang diterima di interval itu).
 * Upload memakai satu span per request yang sudah dijawab server (lihat [UploadSpeedTester]).
 */
data class ByteSpan(val startNanos: Long, val endNanos: Long, val bytes: Long)

/**
 * Perhitungan throughput murni (tanpa I/O), sehingga bisa diuji secara deterministik.
 *
 * Hasil final dihitung dengan cara:
 * 1. Membuang fase ramp-up di awal (dikerjakan pemanggil lewat batas `fromNanos`).
 * 2. Menggeser jendela selebar `windowNanos` per `stepNanos` di rentang analisis,
 *    lalu menghitung Mbps di tiap jendela.
 * 3. Mengambil persentil tertentu dari kumpulan Mbps tersebut.
 */
object ThroughputCalculator {

    /** Mbps dari [bytes] byte dalam [nanos] nanodetik. Satuan desimal (1 Mbit = 1e6 bit). */
    fun mbps(bytes: Double, nanos: Long): Double =
        if (nanos <= 0) 0.0 else bytes * BITS_PER_BYTE / (nanos / NANOS_PER_SEC) / BITS_PER_MBIT

    /** Total byte dari [spans] yang jatuh di dalam rentang [fromNanos]..[toNanos]. */
    fun bytesBetween(spans: List<ByteSpan>, fromNanos: Long, toNanos: Long): Double = spans.sumOf { span ->
        val duration = span.endNanos - span.startNanos
        if (duration <= 0) {
            // Span tanpa durasi diperlakukan sebagai titik di endNanos.
            if (span.endNanos in fromNanos until toNanos) span.bytes.toDouble() else 0.0
        } else {
            val overlap = minOf(span.endNanos, toNanos) - maxOf(span.startNanos, fromNanos)
            if (overlap <= 0) 0.0 else span.bytes.toDouble() * overlap / duration
        }
    }

    /**
     * Mbps di setiap jendela geser pada rentang [fromNanos]..[toNanos]. Bila rentang
     * lebih pendek dari satu jendela, seluruh rentang dihitung sebagai satu jendela.
     */
    fun windowRates(
        spans: List<ByteSpan>,
        fromNanos: Long,
        toNanos: Long,
        windowNanos: Long,
        stepNanos: Long,
    ): List<Double> {
        val range = toNanos - fromNanos
        return when {
            range <= 0 -> emptyList()

            range <= windowNanos -> listOf(mbps(bytesBetween(spans, fromNanos, toNanos), range))

            else -> generateSequence(fromNanos) { it + stepNanos.coerceAtLeast(1) }
                .takeWhile { it <= toNanos - windowNanos }
                .map { start -> mbps(bytesBetween(spans, start, start + windowNanos), windowNanos) }
                .toList()
        }
    }

    /** Persentil [p] (0..1) dengan interpolasi linear antar dua nilai terdekat. */
    fun percentile(values: List<Double>, p: Double): Double {
        require(values.isNotEmpty()) { "values tidak boleh kosong" }
        val sorted = values.sorted()
        val rank = p.coerceIn(0.0, 1.0) * (sorted.size - 1)
        val lower = sorted[floor(rank).toInt()]
        val upper = sorted[ceil(rank).toInt()]
        return lower + (upper - lower) * (rank - floor(rank))
    }

    /** Hasil final fase, atau null bila tidak ada data di rentang [fromNanos]..[toNanos]. */
    fun finalMbps(spans: List<ByteSpan>, fromNanos: Long, toNanos: Long, analysis: Analysis): Double? {
        val rates = windowRates(spans, fromNanos, toNanos, analysis.windowNanos, analysis.stepNanos)
        return if (rates.isEmpty()) null else percentile(rates, analysis.percentile)
    }

    /**
     * Parameter perhitungan hasil final.
     *
     * @param windowNanos lebar jendela geser.
     * @param stepNanos jarak geser antar jendela.
     * @param percentile persentil (0..1) dari Mbps per jendela yang dijadikan hasil.
     */
    data class Analysis(val windowNanos: Long, val stepNanos: Long, val percentile: Double)

    private const val NANOS_PER_SEC = 1_000_000_000.0
    private const val BITS_PER_BYTE = 8.0
    private const val BITS_PER_MBIT = 1_000_000.0
}
