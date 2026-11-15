package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject

/** Menyimpan satu hasil test ke riwayat, mengembalikan id barunya. */
class SaveTestResultUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(result: SpeedTestResult): Long =
        historyRepository.saveResult(result)
}
