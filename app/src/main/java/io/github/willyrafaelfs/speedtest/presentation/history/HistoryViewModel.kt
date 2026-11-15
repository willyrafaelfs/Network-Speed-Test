package io.github.willyrafaelfs.speedtest.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.usecase.ClearHistoryUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.DeleteTestResultUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveDailyStatsUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveTestHistoryUseCase
import io.github.willyrafaelfs.speedtest.presentation.component.TrendMetric
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Memaparkan riwayat hasil test dan tren hariannya dari Room sebagai [StateFlow], plus
 * filter jenis jaringan dan aksi hapus. Karena sumbernya Flow Room, layar otomatis
 * ter-update saat ada perubahan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeTestHistory: ObserveTestHistoryUseCase,
    observeDailyStats: ObserveDailyStatsUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val deleteTestResult: DeleteTestResultUseCase,
    private val clearHistory: ClearHistoryUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow<NetworkType?>(null)
    private val trendMetric = MutableStateFlow(TrendMetric.SPEED)

    val uiState: StateFlow<HistoryUiState> = combine(
        // flatMapLatest: query lama dibatalkan begitu filter berganti.
        filter.flatMapLatest { observeTestHistory(it) },
        filter.flatMapLatest { observeDailyStats(it) },
        filter,
        trendMetric,
        observeSettings(),
    ) { results, stats, currentFilter, metric, settings ->
        HistoryUiState(
            results = results,
            dailyStats = stats,
            filter = currentFilter,
            trendMetric = metric,
            speedUnit = settings.speedUnit,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        // Hentikan koleksi 5 detik setelah UI berhenti mengamati (mis. ganti tab),
        // lalu mulai lagi otomatis: hemat resource tanpa kehilangan data.
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HistoryUiState(isLoading = true),
    )

    fun setFilter(networkType: NetworkType?) {
        filter.value = networkType
    }

    fun setTrendMetric(metric: TrendMetric) {
        trendMetric.value = metric
    }

    fun deleteResult(id: Long) = viewModelScope.launch { deleteTestResult(id) }

    fun clearAll() = viewModelScope.launch { clearHistory() }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
