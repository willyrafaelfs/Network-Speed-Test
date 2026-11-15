package io.github.willyrafaelfs.speedtest.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.usecase.DeleteTestResultUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveResultDetailUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import io.github.willyrafaelfs.speedtest.testing.FakeHistoryRepository
import io.github.willyrafaelfs.speedtest.testing.FakeSettingsRepository
import io.github.willyrafaelfs.speedtest.testing.MainDispatcherRule
import io.github.willyrafaelfs.speedtest.testing.testResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Robolectric: SavedStateHandle.toRoute membaca argumen route lewat Bundle Android. */
@RunWith(AndroidJUnit4::class)
class ResultDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val history = FakeHistoryRepository(listOf(testResult(id = 7, downloadMbps = 123.0)))
    private val settings = FakeSettingsRepository()

    private fun viewModel(id: Long) = ResultDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("id" to id)),
        observeDetail = ObserveResultDetailUseCase(history),
        observeSettings = ObserveSettingsUseCase(settings),
        deleteResult = DeleteTestResultUseCase(history),
    )

    @Test
    fun `memuat hasil sesuai id dari route`() = runTest {
        viewModel(7).uiState.test {
            val state = awaitLoaded()
            assertEquals(123.0, state.detail.result.downloadMbps, 1e-9)
            assertEquals(SpeedUnit.MBPS, state.speedUnit)
        }
    }

    @Test
    fun `id yang tidak ada menghasilkan NotFound`() = runTest {
        viewModel(99).uiState.test {
            var state = awaitItem()
            while (state == ResultDetailUiState.Loading) state = awaitItem()
            assertEquals(ResultDetailUiState.NotFound, state)
        }
    }

    @Test
    fun `hapus menghapus hasil lalu memanggil callback`() = runTest {
        val vm = viewModel(7)
        var deleted = false

        vm.delete(onDeleted = { deleted = true })

        assertTrue(deleted)
        assertTrue(history.stored.isEmpty())
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<ResultDetailUiState>.awaitLoaded(): ResultDetailUiState.Loaded {
        var state = awaitItem()
        while (state !is ResultDetailUiState.Loaded) state = awaitItem()
        return state
    }
}
