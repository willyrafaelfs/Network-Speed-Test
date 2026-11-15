package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Mengamati riwayat hasil test (terbaru lebih dulu). */
class ObserveTestHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): Flow<List<SpeedTestResult>> = historyRepository.observeResults()
}
