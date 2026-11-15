package io.github.willyrafaelfs.speedtest.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Preferensi yang dibutuhkan sebelum frame pertama digambar (tema, dynamic color).
 * Null selama DataStore belum terbaca; splash screen ditahan selama itu.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)
}
