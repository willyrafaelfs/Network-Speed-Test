package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Mengamati riwayat hasil test (terbaru lebih dulu), opsional difilter per jenis jaringan. */
class ObserveTestHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(networkType: NetworkType? = null): Flow<List<SpeedTestResult>> =
        historyRepository.observeResults(networkType)
}
