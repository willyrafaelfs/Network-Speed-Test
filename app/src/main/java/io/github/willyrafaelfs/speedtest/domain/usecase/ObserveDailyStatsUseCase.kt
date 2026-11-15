package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tren harian untuk grafik riwayat: rata-rata download, upload, dan ping per hari,
 * dibatasi [maxDays] hari terakhir yang punya data.
 */
class ObserveDailyStatsUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(networkType: NetworkType? = null, maxDays: Int = DEFAULT_MAX_DAYS): Flow<List<DailyStats>> =
        historyRepository.observeDailyStats(networkType).map { it.takeLast(maxDays) }

    private companion object {
        const val DEFAULT_MAX_DAYS = 30
    }
}
