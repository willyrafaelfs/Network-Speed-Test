package io.github.willyrafaelfs.speedtest.presentation.settings

import app.cash.turbine.test
import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.ThemeMode
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.UpdateSettingsUseCase
import io.github.willyrafaelfs.speedtest.testing.FakeSettingsRepository
import io.github.willyrafaelfs.speedtest.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    @Test
    fun `setiap perubahan langsung tersimpan dan terpantul di state`() = runTest {
        // Dibuat di dalam test: viewModelScope harus memakai dispatcher dari MainDispatcherRule.
        val vm = SettingsViewModel(ObserveSettingsUseCase(repository), UpdateSettingsUseCase(repository))
        vm.settings.test {
            assertEquals(AppSettings(), awaitNonNull())

            vm.setTestDuration(15)
            vm.setStreamCount(8)
            vm.setSpeedUnit(SpeedUnit.MEGABYTES_PER_SECOND)
            vm.setThemeMode(ThemeMode.DARK)
            vm.setDynamicColor(true)

            val expected = AppSettings(15, 8, SpeedUnit.MEGABYTES_PER_SECOND, ThemeMode.DARK, dynamicColor = true)
            assertEquals(expected, expectMostRecentItem())
            assertEquals(expected, repository.settings.first())
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<AppSettings?>.awaitNonNull(): AppSettings {
        var item = awaitItem()
        while (item == null) item = awaitItem()
        return item
    }
}
