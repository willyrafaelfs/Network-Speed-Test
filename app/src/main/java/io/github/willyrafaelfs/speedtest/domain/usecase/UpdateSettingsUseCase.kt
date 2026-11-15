package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.repository.SettingsRepository
import javax.inject.Inject

/** Mengubah preferensi user, mis. updateSettings { it.copy(streamCount = 6) }. */
class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)
}
