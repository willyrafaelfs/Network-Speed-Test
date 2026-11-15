package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Mengamati satu hasil beserta jejak kecepatannya. */
class ObserveResultDetailUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(id: Long): Flow<SpeedTestResultDetail?> = historyRepository.observeResult(id)
}
