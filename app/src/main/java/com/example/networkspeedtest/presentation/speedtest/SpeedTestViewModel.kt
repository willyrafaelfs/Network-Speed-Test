package com.example.networkspeedtest.presentation.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkspeedtest.domain.model.SpeedTestPhase
import com.example.networkspeedtest.domain.model.SpeedTestProgress
import com.example.networkspeedtest.domain.model.SpeedTestResult
import com.example.networkspeedtest.domain.usecase.GetNetworkInfoUseCase
import com.example.networkspeedtest.domain.usecase.RunSpeedTestUseCase
import com.example.networkspeedtest.domain.usecase.SaveTestResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mengorkestrasi satu sesi test dan memaparkan [SpeedTestUiState] ke UI.
 *
 * Seluruh pekerjaan berjalan di [viewModelScope] sehingga otomatis dibatalkan
 * saat ViewModel di-clear — mencegah kebocoran coroutine.
 */
@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val runSpeedTest: RunSpeedTestUseCase,
    private val saveTestResult: SaveTestResultUseCase,
    getNetworkInfo: GetNetworkInfoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    private var testJob: Job? = null

    init {
        // Amati info jaringan sepanjang hidup ViewModel dan cerminkan ke state.
        getNetworkInfo()
            .onEach { info -> _uiState.update { it.copy(networkInfo = info) } }
            .launchIn(viewModelScope)
    }

    /** Memulai sesi test baru. Diabaikan bila sedang berjalan. */
    fun startTest() {
        if (_uiState.value.isRunning) return
        testJob = viewModelScope.launch {
            // Reset metrik tapi pertahankan info jaringan yang sudah terdeteksi.
            _uiState.update {
                SpeedTestUiState(
                    isRunning = true,
                    phase = SpeedTestPhase.PING,
                    networkInfo = it.networkInfo,
                )
            }
            runSpeedTest()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            phase = SpeedTestPhase.IDLE,
                            errorMessage = throwable.message ?: "Test gagal dijalankan",
                        )
                    }
                }
                .collect { progress ->
                    _uiState.update { it.applyProgress(progress) }
                    if (progress.phase == SpeedTestPhase.FINISHED) {
                        saveResult(progress)
                    }
                }
        }
    }

    /** Membatalkan test yang sedang berjalan dan kembali ke keadaan idle. */
    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _uiState.update {
            it.copy(isRunning = false, phase = SpeedTestPhase.IDLE, currentSpeedMbps = 0.0)
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    private suspend fun saveResult(progress: SpeedTestProgress) {
        val networkInfo = _uiState.value.networkInfo
        val result = SpeedTestResult(
            timestamp = System.currentTimeMillis(),
            pingMs = progress.pingMs ?: 0.0,
            jitterMs = progress.jitterMs ?: 0.0,
            downloadMbps = progress.downloadMbps ?: 0.0,
            uploadMbps = progress.uploadMbps ?: 0.0,
            networkType = networkInfo.type,
            networkName = networkInfo.networkName,
        )
        // Gagal simpan tidak boleh membuat UI hasil ikut gagal.
        runCatching { saveTestResult(result) }
    }

    /** Menggabungkan progress terbaru ke state; nilai lama dipertahankan bila field null. */
    private fun SpeedTestUiState.applyProgress(progress: SpeedTestProgress) = copy(
        phase = progress.phase,
        isRunning = progress.phase != SpeedTestPhase.FINISHED,
        currentSpeedMbps = progress.currentSpeedMbps,
        progressFraction = progress.progressFraction,
        pingMs = progress.pingMs ?: pingMs,
        jitterMs = progress.jitterMs ?: jitterMs,
        downloadMbps = progress.downloadMbps ?: downloadMbps,
        uploadMbps = progress.uploadMbps ?: uploadMbps,
    )
}
