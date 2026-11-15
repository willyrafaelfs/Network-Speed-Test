package io.github.willyrafaelfs.speedtest.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.ThemeMode
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.UpdateSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Memaparkan preferensi user dan menyimpan setiap perubahan langsung ke DataStore. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    /** Null selama preferensi belum terbaca (biasanya hanya sesaat). */
    val settings: StateFlow<AppSettings?> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun setTestDuration(seconds: Int) = update { it.copy(testDurationSeconds = seconds) }

    fun setStreamCount(count: Int) = update { it.copy(streamCount = count) }

    fun setSpeedUnit(unit: SpeedUnit) = update { it.copy(speedUnit = unit) }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { updateSettings(transform) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
