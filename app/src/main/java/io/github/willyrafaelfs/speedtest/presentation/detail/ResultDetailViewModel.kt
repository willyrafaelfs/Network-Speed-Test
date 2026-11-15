package io.github.willyrafaelfs.speedtest.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.usecase.DeleteTestResultUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveResultDetailUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import io.github.willyrafaelfs.speedtest.presentation.navigation.ResultDetailRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State layar detail. */
sealed interface ResultDetailUiState {
    data object Loading : ResultDetailUiState

    /** Hasil tidak ada (mis. baru dihapus). */
    data object NotFound : ResultDetailUiState

    data class Loaded(val detail: SpeedTestResultDetail, val speedUnit: SpeedUnit) : ResultDetailUiState
}

/** Detail satu hasil; id dibaca dari route type-safe [ResultDetailRoute]. */
@HiltViewModel
class ResultDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveResultDetailUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val deleteResult: DeleteTestResultUseCase,
) : ViewModel() {

    private val resultId = savedStateHandle.toRoute<ResultDetailRoute>().id

    val uiState: StateFlow<ResultDetailUiState> =
        combine(observeDetail(resultId), observeSettings()) { detail, settings ->
            if (detail == null) ResultDetailUiState.NotFound else ResultDetailUiState.Loaded(detail, settings.speedUnit)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ResultDetailUiState.Loading)

    /** Menghapus hasil ini lalu memanggil [onDeleted] (biasanya kembali ke riwayat). */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteResult(resultId)
            onDeleted()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
