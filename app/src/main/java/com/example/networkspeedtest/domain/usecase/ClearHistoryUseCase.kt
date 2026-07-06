package com.example.networkspeedtest.domain.usecase

import com.example.networkspeedtest.domain.repository.HistoryRepository
import javax.inject.Inject

/** Menghapus seluruh riwayat hasil test. */
class ClearHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke() = historyRepository.clearAll()
}
