package com.example.networkspeedtest.domain.usecase

import com.example.networkspeedtest.domain.model.SpeedTestResult
import com.example.networkspeedtest.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Mengamati riwayat hasil test (terbaru lebih dulu). */
class ObserveTestHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): Flow<List<SpeedTestResult>> = historyRepository.observeResults()
}
