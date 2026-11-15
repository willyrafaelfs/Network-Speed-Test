package io.github.willyrafaelfs.speedtest.presentation.detail

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.BufferbloatGrade
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.presentation.component.GradeBadge
import io.github.willyrafaelfs.speedtest.presentation.component.InstrumentPanel
import io.github.willyrafaelfs.speedtest.presentation.component.PrimaryReadout
import io.github.willyrafaelfs.speedtest.presentation.component.ReadoutDivider
import io.github.willyrafaelfs.speedtest.presentation.component.ReadoutRow
import io.github.willyrafaelfs.speedtest.presentation.component.ThroughputChart
import io.github.willyrafaelfs.speedtest.presentation.component.toMetrics
import io.github.willyrafaelfs.speedtest.presentation.format.currentLocale
import io.github.willyrafaelfs.speedtest.presentation.format.formatDecimal
import io.github.willyrafaelfs.speedtest.presentation.format.formatMs
import io.github.willyrafaelfs.speedtest.presentation.format.formatSpeed
import io.github.willyrafaelfs.speedtest.presentation.format.formatTimestamp
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun ResultDetailScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ResultDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.xs)) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(stringResource(R.string.detail_title), style = MaterialTheme.typography.titleLarge)
        }
        when (val current = state) {
            ResultDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            ResultDetailUiState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is ResultDetailUiState.Loaded -> DetailContent(
                detail = current.detail,
                unit = current.speedUnit,
                snackbarHostState = snackbarHostState,
                onDelete = { viewModel.delete(onDeleted = onBack) },
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: SpeedTestResultDetail,
    unit: SpeedUnit,
    snackbarHostState: SnackbarHostState,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardLayer = rememberGraphicsLayer()
    val chooserTitle = stringResource(R.string.share_chooser)
    val shareFailed = stringResource(R.string.share_failed)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Kartu ini yang direkam menjadi gambar saat dibagikan.
        ShareCard(
            detail = detail,
            unit = unit,
            modifier = Modifier.drawWithContent {
                cardLayer.record { this@drawWithContent.drawContent() }
                drawLayer(cardLayer)
            },
        )
        LatencyPanel(detail)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    scope.launch {
                        val shared = shareCard(context, cardLayer, chooserTitle)
                        if (!shared) snackbarHostState.showSnackbar(shareFailed)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) { Text(stringResource(R.string.detail_share)) }
            TextButton(onClick = onDelete, modifier = Modifier.height(56.dp)) {
                Text(stringResource(R.string.history_delete))
            }
        }
    }
}

/** Mengubah kartu menjadi PNG lalu membuka Share sheet; false bila gagal. */
private suspend fun shareCard(context: Context, layer: GraphicsLayer, chooserTitle: String): Boolean = runCatching {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    ResultImageSharer.share(context, bitmap, chooserTitle)
}.isSuccess

/**
 * Ringkasan hasil yang layak dibagikan: tanggal, jenis jaringan, kecepatan, ping, grade,
 * dan grafik. Nama Wi-Fi/operator dan IP publik sengaja tidak ditampilkan di sini.
 */
@Composable
private fun ShareCard(detail: SpeedTestResultDetail, unit: SpeedUnit, modifier: Modifier = Modifier) {
    val result = detail.result
    val colors = NetraTheme.colors
    val speedUnit = stringResource(unit.labelRes())
    InstrumentPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = formatTimestamp(result.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(result.networkType.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.padding(top = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            PrimaryReadout(
                label = stringResource(R.string.metric_download),
                value = formatSpeed(result.downloadMbps, unit),
                unit = speedUnit,
                channelColor = colors.download,
                modifier = Modifier.weight(1f),
            )
            PrimaryReadout(
                label = stringResource(R.string.metric_upload),
                value = formatSpeed(result.uploadMbps, unit),
                unit = speedUnit,
                channelColor = colors.upload,
                modifier = Modifier.weight(1f),
            )
        }
        ReadoutDivider()
        ReadoutRow(
            stringResource(R.string.metric_ping),
            formatMs(result.pingMs),
            stringResource(R.string.unit_ms),
            channelColor = colors.latency,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.metric_bufferbloat),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            GradeBadge(result.bufferbloatGrade)
        }
        if (detail.downloadTrace.isNotEmpty() || detail.uploadTrace.isNotEmpty()) {
            ThroughputChart(
                download = detail.downloadTrace,
                upload = detail.uploadTrace,
                highlightUpload = false,
                unit = unit,
                modifier = Modifier.padding(top = Spacing.lg),
            )
        }
        Text(
            text = stringResource(R.string.share_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.md),
        )
    }
}

/** Jitter, latency saat beban, dan penjelasan grade dalam kalimat biasa. */
@Composable
private fun LatencyPanel(detail: SpeedTestResultDetail, modifier: Modifier = Modifier) {
    val metrics = detail.result.toMetrics()
    val ms = stringResource(R.string.unit_ms)
    InstrumentPanel(modifier = modifier) {
        ReadoutRow(stringResource(R.string.metric_jitter), formatMs(metrics.jitterMs), ms)
        ReadoutRow(stringResource(R.string.metric_loaded_download), formatMs(metrics.downloadLatencyMs), ms)
        ReadoutRow(stringResource(R.string.metric_loaded_upload), formatMs(metrics.uploadLatencyMs), ms)
        Text(
            text = latencyExplanation(detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        if (detail.downloadTrace.isEmpty() && detail.uploadTrace.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_no_trace),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun latencyExplanation(detail: SpeedTestResultDetail): String {
    val result = detail.result
    val worstLoaded = listOfNotNull(result.downloadLatencyMs, result.uploadLatencyMs).maxOrNull()
        ?: return stringResource(R.string.detail_no_loaded_latency)
    val increase = worstLoaded - result.pingMs
    val locale = currentLocale()
    return if (increase > BufferbloatGrade.A_PLUS.maxIncreaseMs) {
        stringResource(R.string.detail_latency_rise, formatDecimal(increase, 0, locale))
    } else {
        stringResource(R.string.detail_latency_stable, formatDecimal(BufferbloatGrade.A_PLUS.maxIncreaseMs, 0, locale))
    }
}
