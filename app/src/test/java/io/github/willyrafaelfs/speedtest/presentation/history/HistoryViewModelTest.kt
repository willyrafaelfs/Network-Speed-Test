package io.github.willyrafaelfs.speedtest.presentation.history

import app.cash.turbine.test
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.usecase.ClearHistoryUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.DeleteTestResultUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveDailyStatsUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveTestHistoryUseCase
import io.github.willyrafaelfs.speedtest.presentation.component.TrendMetric
import io.github.willyrafaelfs.speedtest.testing.FakeHistoryRepository
import io.github.willyrafaelfs.speedtest.testing.FakeSettingsRepository
import io.github.willyrafaelfs.speedtest.testing.MainDispatcherRule
import io.github.willyrafaelfs.speedtest.testing.testResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val wifi = testResult(id = 1, timestamp = DAY_ONE, downloadMbps = 100.0)
    private val cellular =
        testResult(id = 2, timestamp = DAY_TWO, downloadMbps = 40.0, networkType = NetworkType.CELLULAR)
    private val history = FakeHistoryRepository(listOf(wifi, cellular))
    private val settings = FakeSettingsRepository()

    private fun viewModel() = HistoryViewModel(
        observeTestHistory = ObserveTestHistoryUseCase(history),
        observeDailyStats = ObserveDailyStatsUseCase(history),
        observeSettings = ObserveSettingsUseCase(settings),
        deleteTestResult = DeleteTestResultUseCase(history),
        clearHistory = ClearHistoryUseCase(history),
    )

    @Test
    fun `memuat riwayat terbaru lebih dulu beserta tren harian`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertEquals(listOf(2L, 1L), state.results.map { it.id })
            assertEquals(2, state.dailyStats.size)
            assertTrue(state.hasTrend)
            assertFalse(state.isEmpty)
        }
    }

    @Test
    fun `filter jaringan berlaku untuk daftar dan tren`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.setFilter(NetworkType.CELLULAR)

            val state = expectMostRecentItem()
            assertEquals(NetworkType.CELLULAR, state.filter)
            assertEquals(listOf(2L), state.results.map { it.id })
            assertEquals(1, state.dailyStats.size)
            assertFalse("Tren butuh minimal dua hari", state.hasTrend)
            // Kosong karena filter bukan berarti riwayat kosong.
            vm.setFilter(NetworkType.ETHERNET)
            assertFalse(expectMostRecentItem().isEmpty)
        }
    }

    @Test
    fun `metrik tren dan satuan bisa diganti`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.setTrendMetric(TrendMetric.PING)
            settings.update { it.copy(speedUnit = SpeedUnit.MEGABYTES_PER_SECOND) }

            val state = expectMostRecentItem()
            assertEquals(TrendMetric.PING, state.trendMetric)
            assertEquals(SpeedUnit.MEGABYTES_PER_SECOND, state.speedUnit)
        }
    }

    @Test
    fun `hapus satu dan hapus semua memperbarui daftar`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.deleteResult(1)
            assertEquals(listOf(2L), expectMostRecentItem().results.map { it.id })

            vm.clearAll()
            assertTrue(expectMostRecentItem().isEmpty)
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<HistoryUiState>.awaitLoaded(): HistoryUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    private companion object {
        // 14 dan 15 November 2026 siang hari (UTC), cukup jauh dari pergantian hari di zona waktu mana pun.
        const val DAY_ONE = 1_763_121_600_000
        const val DAY_TWO = 1_763_208_000_000
    }
}
