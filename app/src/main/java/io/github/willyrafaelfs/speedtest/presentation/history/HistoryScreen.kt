package io.github.willyrafaelfs.speedtest.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.presentation.component.GradeBadge
import io.github.willyrafaelfs.speedtest.presentation.component.InstrumentPanel
import io.github.willyrafaelfs.speedtest.presentation.component.PrimaryReadout
import io.github.willyrafaelfs.speedtest.presentation.component.TrendChart
import io.github.willyrafaelfs.speedtest.presentation.component.TrendMetric
import io.github.willyrafaelfs.speedtest.presentation.format.formatMs
import io.github.willyrafaelfs.speedtest.presentation.format.formatSpeed
import io.github.willyrafaelfs.speedtest.presentation.format.formatTimestamp
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

@Composable
fun HistoryScreen(
    onStartTest: () -> Unit,
    onOpenResult: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        actions = HistoryActions(
            onClearAll = viewModel::clearAll,
            onStartTest = onStartTest,
            onOpenResult = onOpenResult,
            onFilterChange = viewModel::setFilter,
            onTrendMetricChange = viewModel::setTrendMetric,
        ),
        modifier = modifier,
    )
}

/** Callback layar riwayat, dikelompokkan agar content tetap ringkas. */
data class HistoryActions(
    val onClearAll: () -> Unit = {},
    val onStartTest: () -> Unit = {},
    val onOpenResult: (Long) -> Unit = {},
    val onFilterChange: (NetworkType?) -> Unit = {},
    val onTrendMetricChange: (TrendMetric) -> Unit = {},
)

@Composable
private fun HistoryContent(state: HistoryUiState, actions: HistoryActions, modifier: Modifier = Modifier) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.isEmpty -> EmptyHistory(onStartTest = actions.onStartTest, modifier = modifier)

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "header") {
                HistoryHeader(count = state.results.size, onClearAll = { showClearDialog = true })
            }
            item(key = "filters") {
                FilterRow(selected = state.filter, onSelect = actions.onFilterChange)
            }
            item(key = "trend") {
                TrendPanel(state = state, onMetricChange = actions.onTrendMetricChange)
            }
            if (state.results.isEmpty()) {
                item(key = "filtered-empty") {
                    Text(
                        text = stringResource(R.string.history_filtered_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.lg),
                    )
                }
            }
            items(state.results, key = { it.id }) { result ->
                HistoryEntry(result = result, unit = state.speedUnit, onClick = { actions.onOpenResult(result.id) })
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onClearAll()
                    showClearDialog = false
                }) { Text(stringResource(R.string.history_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun HistoryHeader(count: Int, onClearAll: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = pluralStringResource(R.plurals.history_count, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClearAll) { Text(stringResource(R.string.history_clear)) }
    }
}

@Composable
private fun FilterRow(selected: NetworkType?, onSelect: (NetworkType?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HistoryUiState.FILTERS.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter?.labelRes() ?: R.string.filter_all)) },
            )
        }
    }
}

@Composable
private fun TrendPanel(state: HistoryUiState, onMetricChange: (TrendMetric) -> Unit) {
    InstrumentPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.trend_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TrendMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = metric == state.trendMetric,
                        onClick = { onMetricChange(metric) },
                        label = {
                            Text(
                                stringResource(
                                    if (metric ==
                                        TrendMetric.SPEED
                                    ) {
                                        R.string.trend_metric_speed
                                    } else {
                                        R.string.trend_metric_ping
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        if (state.hasTrend) {
            TrendChart(stats = state.dailyStats, metric = state.trendMetric, unit = state.speedUnit)
        } else {
            Text(
                text = stringResource(R.string.trend_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.md),
            )
        }
    }
}

@Composable
private fun EmptyHistory(onStartTest: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.history_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Button(onClick = onStartTest, shape = MaterialTheme.shapes.medium) {
                Text(stringResource(R.string.action_start_test))
            }
        }
    }
}

@Composable
private fun HistoryEntry(result: SpeedTestResult, unit: SpeedUnit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NetraTheme.colors
    val speedUnit = stringResource(unit.labelRes())
    val timestamp = formatTimestamp(result.timestamp)
    val openDescription = stringResource(R.string.cd_open_result, timestamp)

    InstrumentPanel(modifier = modifier.semantics { contentDescription = openDescription }, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = timestamp, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                text = result.networkName ?: stringResource(networkTypeLabel(result.networkType)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.padding(top = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            val compact = NetraTheme.numerals.readoutSmall
            PrimaryReadout(
                label = stringResource(R.string.metric_download),
                value = formatSpeed(result.downloadMbps, unit),
                unit = speedUnit,
                channelColor = colors.download,
                valueStyle = compact,
                modifier = Modifier.weight(1f),
            )
            PrimaryReadout(
                label = stringResource(R.string.metric_upload),
                value = formatSpeed(result.uploadMbps, unit),
                unit = speedUnit,
                channelColor = colors.upload,
                valueStyle = compact,
                modifier = Modifier.weight(1f),
            )
            PrimaryReadout(
                label = stringResource(R.string.metric_ping),
                value = formatMs(result.pingMs),
                unit = stringResource(R.string.unit_ms),
                channelColor = colors.latency,
                valueStyle = compact,
                modifier = Modifier.weight(1f),
            )
            // Hasil lama (sebelum latency saat beban diukur) tidak punya grade.
            result.bufferbloatGrade?.let { GradeBadge(it) }
        }
    }
}

private fun networkTypeLabel(type: NetworkType): Int = if (type == NetworkType.NONE) {
    R.string.network_unknown
} else {
    type.labelRes()
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1220, heightDp = 900)
@Composable
private fun HistoryContentPreview() {
    NetraTheme(darkTheme = true) {
        HistoryContent(
            state = HistoryUiState(
                isLoading = false,
                dailyStats = List(6) { DailyStats("2026-11-${it + 10}", 60.0 + it * 5, 9.0 + it, 90.0 - it * 4, 2) },
                results = listOf(
                    SpeedTestResult(
                        id = 1,
                        timestamp = 1_763_200_000_000,
                        pingMs = 18.0,
                        jitterMs = 2.0,
                        downloadMbps = 187.4,
                        uploadMbps = 41.2,
                        networkType = NetworkType.WIFI,
                        networkName = "Rumah",
                        downloadLatencyMs = 62.0,
                        uploadLatencyMs = 45.0,
                    ),
                ),
            ),
            actions = HistoryActions(),
        )
    }
}
