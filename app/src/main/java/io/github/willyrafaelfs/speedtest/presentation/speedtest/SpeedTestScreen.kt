package io.github.willyrafaelfs.speedtest.presentation.speedtest

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import io.github.willyrafaelfs.speedtest.presentation.component.MetricCard
import io.github.willyrafaelfs.speedtest.presentation.component.NetworkInfoCard
import io.github.willyrafaelfs.speedtest.presentation.component.SpeedGauge
import io.github.willyrafaelfs.speedtest.presentation.component.label
import io.github.willyrafaelfs.speedtest.ui.theme.NetworkSpeedTestTheme
import java.util.Locale

/** Layar utama: memegang ViewModel dan meneruskan state ke content yang stateless. */
@Composable
fun SpeedTestScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SpeedTestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(hasFineLocation(context)) }
    // FINE dan COARSE diminta bersamaan (wajib sejak Android 12). SSID hanya bisa
    // dibaca dengan lokasi presisi, jadi yang menentukan adalah hasil FINE.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results -> hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    SpeedTestContent(
        state = state,
        snackbarHostState = snackbarHostState,
        canReadWifiName = hasLocationPermission,
        onRequestLocationPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onStart = viewModel::startTest,
        onCancel = viewModel::cancelTest,
        onErrorDismiss = viewModel::dismissError,
        modifier = modifier,
    )
}

private fun hasFineLocation(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

/**
 * Content stateless — mudah di-preview dan diuji tanpa Hilt.
 */
@Composable
private fun SpeedTestContent(
    state: SpeedTestUiState,
    snackbarHostState: SnackbarHostState,
    canReadWifiName: Boolean,
    onRequestLocationPermission: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Simpan callback terbaru agar effect (yang hanya restart saat pesan berubah)
    // tidak memanggil lambda lama bila parent recompose dengan lambda baru.
    val currentOnErrorDismiss by rememberUpdatedState(onErrorDismiss)
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(errorMessage(error))
            currentOnErrorDismiss()
        }
    }

    // Tanpa Scaffold sendiri: background & snackbar disediakan Scaffold root (MainScreen).
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Network Speed Test",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = phaseLabel(state.phase),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Saat selesai, gauge menampilkan hasil download; selain itu kecepatan sesaat.
        val gaugeSpeed = if (state.isFinished) {
            state.downloadMbps ?: 0.0
        } else {
            state.currentSpeedMbps
        }
        SpeedGauge(speedMbps = gaugeSpeed.toFloat())

        // Progress fase saat ini (hanya saat berjalan).
        if (state.isRunning) {
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        MetricsGrid(state = state)

        NetworkInfoCard(
            networkInfo = state.networkInfo,
            canReadWifiName = canReadWifiName,
            onRequestLocationPermission = onRequestLocationPermission,
        )

        Spacer(Modifier.height(4.dp))

        if (state.isRunning) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Batal")
            }
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isFinished) "Test Ulang" else "Mulai Test")
            }
        }
    }
}

@Composable
private fun MetricsGrid(state: SpeedTestUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Ping",
                value = formatMs(state.pingMs),
                unit = "ms",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Jitter",
                value = formatMs(state.jitterMs),
                unit = "ms",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Download",
                value = formatSpeed(state.downloadMbps),
                unit = "Mbps",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Upload",
                value = formatSpeed(state.uploadMbps),
                unit = "Mbps",
                modifier = Modifier.weight(1f),
            )
        }
        // Latency saat beban: ping yang diukur selama download/upload berjalan.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Ping saat ↓",
                value = formatMs(state.downloadLatencyMs),
                unit = "ms",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Ping saat ↑",
                value = formatMs(state.uploadLatencyMs),
                unit = "ms",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Bufferbloat",
                value = state.bufferbloatGrade?.label ?: "—",
                unit = "grade",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun phaseLabel(phase: SpeedTestPhase): String = when (phase) {
    SpeedTestPhase.IDLE -> "Siap menguji"
    SpeedTestPhase.PING -> "Mengukur ping…"
    SpeedTestPhase.DOWNLOAD -> "Mengukur download…"
    SpeedTestPhase.UPLOAD -> "Mengukur upload…"
    SpeedTestPhase.FINISHED -> "Test selesai"
}

private fun errorMessage(error: SpeedTestError): String = when (error) {
    SpeedTestError.NoConnection -> "Tidak ada koneksi internet atau server tidak terjangkau."
    SpeedTestError.Timeout -> "Server terlalu lama merespons. Coba lagi beberapa saat lagi."
    is SpeedTestError.ServerError -> "Server menolak permintaan (HTTP ${error.httpCode}). Coba lagi nanti."
    SpeedTestError.Unknown -> "Test gagal karena kesalahan yang tidak terduga."
}

private fun formatSpeed(value: Double?): String =
    value?.let { String.format(Locale.US, "%.1f", it) } ?: "—"

private fun formatMs(value: Double?): String =
    value?.let { String.format(Locale.US, "%.0f", it) } ?: "—"

@Preview(showBackground = true)
@Composable
private fun SpeedTestContentPreview() {
    NetworkSpeedTestTheme {
        SpeedTestContent(
            state = SpeedTestUiState(
                phase = SpeedTestPhase.FINISHED,
                pingMs = 24.0,
                jitterMs = 3.0,
                downloadMbps = 87.4,
                uploadMbps = 41.2,
                downloadLatencyMs = 61.0,
                uploadLatencyMs = 48.0,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            canReadWifiName = true,
            onRequestLocationPermission = {},
            onStart = {},
            onCancel = {},
            onErrorDismiss = {},
        )
    }
}
