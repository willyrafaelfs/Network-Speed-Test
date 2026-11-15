package io.github.willyrafaelfs.speedtest.presentation.history

import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.presentation.component.TrendMetric

/**
 * State layar riwayat.
 *
 * @param results daftar hasil test sesuai [filter] (terbaru lebih dulu).
 * @param dailyStats rata-rata harian sesuai [filter], untuk grafik tren.
 * @param filter jenis jaringan yang ditampilkan; null = semua.
 * @param trendMetric metrik yang sedang ditampilkan grafik tren.
 * @param speedUnit satuan tampilan kecepatan dari Settings.
 * @param isLoading true selama data pertama belum termuat dari Room.
 */
data class HistoryUiState(
    val results: List<SpeedTestResult> = emptyList(),
    val dailyStats: List<DailyStats> = emptyList(),
    val filter: NetworkType? = null,
    val trendMetric: TrendMetric = TrendMetric.SPEED,
    val speedUnit: SpeedUnit = SpeedUnit.MBPS,
    val isLoading: Boolean = true,
) {
    /** Belum ada hasil sama sekali (bukan sekadar kosong karena filter). */
    val isEmpty: Boolean get() = !isLoading && results.isEmpty() && filter == null

    /** Tren baru bermakna bila ada data dari minimal dua hari. */
    val hasTrend: Boolean get() = dailyStats.size >= MIN_TREND_DAYS

    companion object {
        /** Filter yang ditawarkan di layar riwayat. */
        val FILTERS: List<NetworkType?> = listOf(null, NetworkType.WIFI, NetworkType.CELLULAR)

        private const val MIN_TREND_DAYS = 2
    }
}
