package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject

/** Menghapus satu hasil test dari riwayat berdasarkan id. */
class DeleteTestResultUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(id: Long) = historyRepository.deleteResult(id)
}
