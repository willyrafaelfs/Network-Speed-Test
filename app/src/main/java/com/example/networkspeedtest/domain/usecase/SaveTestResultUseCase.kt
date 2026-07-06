package com.example.networkspeedtest.domain.usecase

import com.example.networkspeedtest.domain.model.SpeedTestResult
import com.example.networkspeedtest.domain.repository.HistoryRepository
import javax.inject.Inject

/** Menyimpan satu hasil test ke riwayat, mengembalikan id barunya. */
class SaveTestResultUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(result: SpeedTestResult): Long =
        historyRepository.saveResult(result)
}
