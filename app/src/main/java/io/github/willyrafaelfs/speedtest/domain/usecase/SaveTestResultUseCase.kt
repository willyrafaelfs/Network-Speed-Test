package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject

/** Menyimpan satu hasil test (beserta jejak kecepatannya) ke riwayat, mengembalikan id barunya. */
class SaveTestResultUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(
        result: SpeedTestResult,
        downloadTrace: List<TracePoint> = emptyList(),
        uploadTrace: List<TracePoint> = emptyList(),
    ): Long = historyRepository.saveResult(result, downloadTrace, uploadTrace)
}
