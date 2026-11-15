package io.github.willyrafaelfs.speedtest.presentation.speedtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.presentation.component.GradeBadge
import io.github.willyrafaelfs.speedtest.presentation.component.InstrumentDial
import io.github.willyrafaelfs.speedtest.presentation.component.InstrumentPanel
import io.github.willyrafaelfs.speedtest.presentation.component.LocationAccess
import io.github.willyrafaelfs.speedtest.presentation.component.NetworkPanel
import io.github.willyrafaelfs.speedtest.presentation.component.PhaseStepper
import io.github.willyrafaelfs.speedtest.presentation.component.PrimaryReadout
import io.github.willyrafaelfs.speedtest.presentation.component.ReadoutDivider
import io.github.willyrafaelfs.speedtest.presentation.component.ReadoutRow
import io.github.willyrafaelfs.speedtest.presentation.component.ResultReadoutsPanel
import io.github.willyrafaelfs.speedtest.presentation.component.ThroughputChart
import io.github.willyrafaelfs.speedtest.presentation.format.formatMs
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.presentation.format.message
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/** Layar utama: memegang ViewModel & izin lokasi, lalu meneruskan state ke content yang stateless. */
@Composable
fun SpeedTestScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SpeedTestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val location = rememberLocationPermissionState()
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    // Dua kolom mulai dari lebar "medium" (tablet, ponsel landscape, foldable terbuka).
    val twoPane = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    // Di layar pendek (ponsel landscape) dial diperkecil agar tombol tetap terlihat.
    val dialMaxSize = if (sizeClass.isHeightAtLeastBreakpoint(
            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
        )
    ) {
        300.dp
    } else {
        220.dp
    }

    SpeedTestContent(
        state = state,
        snackbarHostState = snackbarHostState,
        locationAccess = location.access,
        twoPane = twoPane,
        dialMaxSize = dialMaxSize,
        onStart = viewModel::startTest,
        onCancel = viewModel::cancelTest,
        onErrorDismiss = viewModel::dismissError,
        onRequestLocation = location.onRequest,
        onOpenSettings = location.onOpenSettings,
        modifier = modifier,
    )

    if (location.isRationaleVisible) {
        AlertDialog(
            onDismissRequest = location.onDismissRationale,
            title = { Text(stringResource(R.string.permission_rationale_title)) },
            text = { Text(stringResource(R.string.permission_rationale_body)) },
            confirmButton = {
                TextButton(onClick = location.onConfirmRationale) { Text(stringResource(R.string.permission_continue)) }
            },
            dismissButton = {
                TextButton(onClick = location.onDismissRationale) { Text(stringResource(R.string.permission_not_now)) }
            },
        )
    }
}

/** Content stateless — mudah di-preview dan diuji tanpa Hilt. */
@Composable
private fun SpeedTestContent(
    state: SpeedTestUiState,
    snackbarHostState: SnackbarHostState,
    locationAccess: LocationAccess,
    twoPane: Boolean,
    dialMaxSize: Dp,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onErrorDismiss: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Simpan callback terbaru agar effect (yang hanya restart saat pesan berubah)
    // tidak memanggil lambda lama bila parent recompose dengan lambda baru.
    val currentOnErrorDismiss by rememberUpdatedState(onErrorDismiss)
    val errorMessage = state.error?.message()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            currentOnErrorDismiss()
        }
    }

    val instrument: @Composable () -> Unit = {
        InstrumentColumn(state = state, dialMaxSize = dialMaxSize, onStart = onStart, onCancel = onCancel)
    }
    val details: @Composable () -> Unit = {
        DetailsColumn(
            state = state,
            locationAccess = locationAccess,
            onRequestLocation = onRequestLocation,
            onOpenSettings = onOpenSettings,
        )
    }

    if (twoPane) {
        Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            ScrollingColumn(Modifier.weight(1f)) { instrument() }
            ScrollingColumn(Modifier.weight(1f)) { details() }
        }
    } else {
        ScrollingColumn(modifier.fillMaxSize()) {
            instrument()
            details()
        }
    }
}

@Composable
private fun ScrollingColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) { content() }
}

/** Dial, stepper fase, dan tombol aksi: bagian yang dilihat selama test berjalan. */
@Composable
private fun InstrumentColumn(
    state: SpeedTestUiState,
    dialMaxSize: Dp,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        InstrumentColumnContent(state, dialMaxSize, onStart, onCancel)
    }
}

@Composable
private fun ColumnScope.InstrumentColumnContent(
    state: SpeedTestUiState,
    dialMaxSize: Dp,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        InstrumentDial(
            speedMbps = (if (state.isFinished) state.downloadMbps ?: 0.0 else state.currentSpeedMbps).toFloat(),
            channelColor = channelColor(state.phase),
            caption = stringResource(state.phase.labelRes()),
            unit = state.speedUnit,
            maxSize = dialMaxSize,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    PhaseStepper(phase = state.phase, progressFraction = state.progressFraction)
    ActionButton(state = state, onStart = onStart, onCancel = onCancel)
}

@Composable
private fun ActionButton(state: SpeedTestUiState, onStart: () -> Unit, onCancel: () -> Unit) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
    if (state.isRunning) {
        OutlinedButton(onClick = onCancel, modifier = buttonModifier, shape = MaterialTheme.shapes.medium) {
            Text(stringResource(R.string.action_stop_test))
        }
    } else {
        Button(onClick = onStart, modifier = buttonModifier, shape = MaterialTheme.shapes.medium) {
            Text(stringResource(if (state.isFinished) R.string.action_test_again else R.string.action_start_test))
        }
    }
}

/** Grafik, readout hasil, dan info jaringan. */
@Composable
private fun DetailsColumn(
    state: SpeedTestUiState,
    locationAccess: LocationAccess,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        DetailsColumnContent(state, locationAccess, onRequestLocation, onOpenSettings)
    }
}

@Composable
private fun ColumnScope.DetailsColumnContent(
    state: SpeedTestUiState,
    locationAccess: LocationAccess,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    InstrumentPanel {
        ThroughputChart(
            download = state.downloadTrace,
            upload = state.uploadTrace,
            highlightUpload = state.phase == SpeedTestPhase.UPLOAD || state.uploadTrace.isNotEmpty(),
            unit = state.speedUnit,
        )
    }
    ResultReadoutsPanel(metrics = state.metrics, unit = state.speedUnit)
    NetworkPanel(
        networkInfo = state.networkInfo,
        locationAccess = locationAccess,
        onRequestLocation = onRequestLocation,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun channelColor(phase: SpeedTestPhase): Color = when (phase) {
    SpeedTestPhase.PING -> NetraTheme.colors.latency
    SpeedTestPhase.UPLOAD -> NetraTheme.colors.upload
    else -> NetraTheme.colors.download
}

private val PreviewState = SpeedTestUiState(
    phase = SpeedTestPhase.FINISHED,
    pingMs = 24.0,
    jitterMs = 3.0,
    downloadMbps = 187.4,
    uploadMbps = 41.2,
    downloadLatencyMs = 61.0,
    uploadLatencyMs = 48.0,
    downloadTrace = List(36) { TracePoint(it / 35f, 150f + (it % 6) * 12f) },
    uploadTrace = List(36) { TracePoint(it / 35f, 36f + (it % 4) * 3f) },
    networkInfo = NetworkInfo(NetworkType.WIFI, "Rumah", "203.0.113.7", isConnected = true),
)

@Preview(showBackground = true, backgroundColor = 0xFF0B1220, heightDp = 1400)
@Composable
private fun SpeedTestContentPreview() {
    NetraTheme(darkTheme = true) {
        SpeedTestContent(
            state = PreviewState,
            snackbarHostState = remember { SnackbarHostState() },
            locationAccess = LocationAccess.GRANTED,
            twoPane = false,
            dialMaxSize = 300.dp,
            onStart = {},
            onCancel = {},
            onErrorDismiss = {},
            onRequestLocation = {},
            onOpenSettings = {},
        )
    }
}
