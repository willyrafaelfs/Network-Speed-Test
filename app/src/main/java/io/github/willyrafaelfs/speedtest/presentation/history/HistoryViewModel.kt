package io.github.willyrafaelfs.speedtest.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.willyrafaelfs.speedtest.domain.usecase.ClearHistoryUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.DeleteTestResultUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveTestHistoryUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Memaparkan riwayat hasil test dari Room sebagai [StateFlow], plus aksi
 * hapus satu / hapus semua. Karena sumbernya Flow Room, daftar otomatis
 * ter-update saat ada perubahan.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeTestHistory: ObserveTestHistoryUseCase,
    private val deleteTestResult: DeleteTestResultUseCase,
    private val clearHistory: ClearHistoryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeTestHistory()
        .map { results -> HistoryUiState(results = results, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            // Hentikan koleksi 5 detik setelah UI berhenti mengamati (mis. ganti tab),
            // lalu mulai lagi otomatis — hemat resource tanpa kehilangan data.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HistoryUiState(isLoading = true),
        )

    fun deleteResult(id: Long) = viewModelScope.launch { deleteTestResult(id) }

    fun clearAll() = viewModelScope.launch { clearHistory() }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
