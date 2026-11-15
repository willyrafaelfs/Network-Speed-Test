package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.BufferbloatGrade
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.presentation.format.formatMs
import io.github.willyrafaelfs.speedtest.presentation.format.formatSpeed
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/**
 * Panel hasil bergaya instrumen: download & upload sebagai nilai utama, lalu ping, jitter,
 * latency saat beban, dan grade bufferbloat.
 */
@Composable
fun ResultReadoutsPanel(metrics: ResultMetrics, unit: SpeedUnit, modifier: Modifier = Modifier) {
    val colors = NetraTheme.colors
    val speedUnit = stringResource(unit.labelRes())
    val ms = stringResource(R.string.unit_ms)
    InstrumentPanel(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            PrimaryReadout(
                label = stringResource(R.string.metric_download),
                value = formatSpeed(metrics.downloadMbps, unit),
                unit = speedUnit,
                channelColor = colors.download,
                modifier = Modifier.weight(1f),
            )
            PrimaryReadout(
                label = stringResource(R.string.metric_upload),
                value = formatSpeed(metrics.uploadMbps, unit),
                unit = speedUnit,
                channelColor = colors.upload,
                modifier = Modifier.weight(1f),
            )
        }
        ReadoutDivider()
        ReadoutRow(stringResource(R.string.metric_ping), formatMs(metrics.pingMs), ms, channelColor = colors.latency)
        ReadoutRow(stringResource(R.string.metric_jitter), formatMs(metrics.jitterMs), ms)
        ReadoutRow(stringResource(R.string.metric_loaded_download), formatMs(metrics.downloadLatencyMs), ms)
        ReadoutRow(stringResource(R.string.metric_loaded_upload), formatMs(metrics.uploadLatencyMs), ms)
        ReadoutDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.metric_bufferbloat),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            GradeBadge(metrics.bufferbloatGrade)
        }
    }
}
