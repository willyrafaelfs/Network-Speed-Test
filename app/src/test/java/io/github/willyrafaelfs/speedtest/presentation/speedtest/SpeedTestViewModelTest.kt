package io.github.willyrafaelfs.speedtest.presentation.speedtest

import app.cash.turbine.test
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestException
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.domain.usecase.GetNetworkInfoUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.ObserveSettingsUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.RunSpeedTestUseCase
import io.github.willyrafaelfs.speedtest.domain.usecase.SaveTestResultUseCase
import io.github.willyrafaelfs.speedtest.testing.FakeHistoryRepository
import io.github.willyrafaelfs.speedtest.testing.FakeNetworkInfoRepository
import io.github.willyrafaelfs.speedtest.testing.FakeSettingsRepository
import io.github.willyrafaelfs.speedtest.testing.FakeSpeedTestRepository
import io.github.willyrafaelfs.speedtest.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedTestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val speedTest = FakeSpeedTestRepository()
    private val history = FakeHistoryRepository()
    private val settings = FakeSettingsRepository()
    private val network = FakeNetworkInfoRepository()

    private fun viewModel() = SpeedTestViewModel(
        runSpeedTest = RunSpeedTestUseCase(speedTest, settings),
        saveTestResult = SaveTestResultUseCase(history),
        getNetworkInfo = GetNetworkInfoUseCase(network),
        observeSettings = ObserveSettingsUseCase(settings),
    )

    @Test
    fun `test yang berhasil berakhir FINISHED dan menyimpan hasil beserta jejaknya`() = runTest {
        val vm = viewModel()

        vm.startTest()

        val state = vm.uiState.value
        assertEquals(SpeedTestPhase.FINISHED, state.phase)
        assertFalse(state.isRunning)
        assertEquals(92.5, state.downloadMbps!!, DELTA)
        assertEquals(33.0, state.uploadMbps!!, DELTA)
        assertEquals(20.0, state.pingMs!!, DELTA)

        val saved = history.stored.single()
        assertEquals(92.5, saved.result.downloadMbps, DELTA)
        assertEquals(NetworkType.WIFI, saved.result.networkType)
        assertEquals(60.0, saved.result.downloadLatencyMs!!, DELTA)
        // Sampel ringkasan (progress 1f) tidak masuk jejak.
        assertEquals(listOf(TracePoint(0.5f, 80f)), saved.downloadTrace)
        assertEquals(listOf(TracePoint(0.5f, 30f)), saved.uploadTrace)
    }

    @Test
    fun `state melewati fase ping lalu download lalu upload lalu selesai`() = runTest {
        speedTest.sampleDelayMillis = 100
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(SpeedTestPhase.IDLE, awaitItem().phase)
            vm.startTest()
            val phases = mutableListOf<SpeedTestPhase>()
            do {
                val state = awaitItem()
                if (phases.lastOrNull() != state.phase) phases += state.phase
            } while (state.phase != SpeedTestPhase.FINISHED)

            assertEquals(
                listOf(SpeedTestPhase.PING, SpeedTestPhase.DOWNLOAD, SpeedTestPhase.UPLOAD, SpeedTestPhase.FINISHED),
                phases,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error di tengah test kembali ke IDLE dengan pesan dan tidak menyimpan apa pun`() = runTest {
        speedTest.downloadFailure = SpeedTestException(SpeedTestError.Timeout)
        val vm = viewModel()

        vm.startTest()

        val state = vm.uiState.value
        assertEquals(SpeedTestPhase.IDLE, state.phase)
        assertFalse(state.isRunning)
        assertEquals(SpeedTestError.Timeout, state.error)
        assertTrue(history.stored.isEmpty())

        vm.dismissError()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `pembatalan menghentikan test tanpa error dan tanpa menyimpan`() = runTest {
        speedTest.sampleDelayMillis = 1_000
        val vm = viewModel()

        vm.startTest()
        advanceTimeBy(1_500) // di tengah fase download
        assertEquals(SpeedTestPhase.DOWNLOAD, vm.uiState.value.phase)

        vm.cancelTest()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(SpeedTestPhase.IDLE, state.phase)
        assertFalse(state.isRunning)
        assertNull(state.error)
        assertTrue(history.stored.isEmpty())
    }

    @Test
    fun `gagal menyimpan ke riwayat tidak membuat hasil di layar ikut gagal`() = runTest {
        history.failOnSave = true
        val vm = viewModel()

        vm.startTest()

        assertEquals(SpeedTestPhase.FINISHED, vm.uiState.value.phase)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `menekan mulai saat test berjalan diabaikan`() = runTest {
        speedTest.sampleDelayMillis = 1_000
        val vm = viewModel()

        vm.startTest()
        vm.startTest()
        advanceUntilIdle()

        assertEquals(1, history.stored.size)
    }

    @Test
    fun `satuan dan info jaringan mengikuti Settings dan koneksi terkini`() = runTest {
        val vm = viewModel()

        settings.update { it.copy(speedUnit = SpeedUnit.MEGABYTES_PER_SECOND) }
        network.info.value = network.info.value.copy(type = NetworkType.CELLULAR, networkName = "Operator")

        assertEquals(SpeedUnit.MEGABYTES_PER_SECOND, vm.uiState.value.speedUnit)
        assertEquals(NetworkType.CELLULAR, vm.uiState.value.networkInfo.type)
    }

    private companion object {
        const val DELTA = 1e-9
    }
}
