package com.example.networkspeedtest.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkspeedtest.domain.usecase.ClearHistoryUseCase
import com.example.networkspeedtest.domain.usecase.DeleteTestResultUseCase
import com.example.networkspeedtest.domain.usecase.ObserveTestHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(isLoading = true),
        )

    fun deleteResult(id: Long) = viewModelScope.launch { deleteTestResult(id) }

    fun clearAll() = viewModelScope.launch { clearHistory() }
}
