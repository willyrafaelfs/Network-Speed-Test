package io.github.willyrafaelfs.speedtest.testing

import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** SettingsRepository in-memory untuk test. */
class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: StateFlow<AppSettings> = state

    override suspend fun update(transform: (AppSettings) -> AppSettings) = state.update(transform)
}
