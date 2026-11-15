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
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.presentation.component.GradeBadge
import io.github.willyrafaelfs.speedtest.presentation.component.InstrumentPanel
import io.github.willyrafaelfs.speedtest.presentation.component.PrimaryReadout
import io.github.willyrafaelfs.speedtest.presentation.format.currentLocale
import io.github.willyrafaelfs.speedtest.presentation.format.formatMbps
import io.github.willyrafaelfs.speedtest.presentation.format.formatMs
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onDelete = viewModel::deleteResult,
        onClearAll = viewModel::clearAll,
        onStartTest = onStartTest,
        modifier = modifier,
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.isEmpty -> EmptyHistory(onStartTest = onStartTest, modifier = modifier)

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "header") {
                HistoryHeader(count = state.results.size, onClearAll = { showClearDialog = true })
            }
            items(state.results, key = { it.id }) { result ->
                HistoryEntry(result = result, onDelete = { onDelete(result.id) })
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
                    onClearAll()
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
private fun HistoryEntry(result: SpeedTestResult, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NetraTheme.colors
    val mbps = stringResource(R.string.unit_mbps)
    val timestamp = formatTimestamp(result.timestamp)
    val deleteDescription = stringResource(R.string.cd_history_delete, timestamp)

    InstrumentPanel(modifier = modifier) {
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
        ) {
            val compact = NetraTheme.numerals.readoutSmall
            PrimaryReadout(
                label = stringResource(R.string.metric_download),
                value = formatMbps(result.downloadMbps),
                unit = mbps,
                channelColor = colors.download,
                valueStyle = compact,
                modifier = Modifier.weight(1f),
            )
            PrimaryReadout(
                label = stringResource(R.string.metric_upload),
                value = formatMbps(result.uploadMbps),
                unit = mbps,
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
        }
        Row(modifier = Modifier.padding(top = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            // Hasil lama (sebelum latency saat beban diukur) tidak punya grade.
            result.bufferbloatGrade?.let { grade ->
                Text(
                    text = stringResource(R.string.metric_bufferbloat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = Spacing.sm),
                )
                GradeBadge(grade)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = deleteDescription }) {
                Text(stringResource(R.string.history_delete))
            }
        }
    }
}

/** Tanggal & jam sesuai locale UI; formatter dibuat per panggilan karena DateFormat tidak thread-safe. */
@Composable
private fun formatTimestamp(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale()).format(Date(timestamp))

private fun networkTypeLabel(type: NetworkType): Int = if (type == NetworkType.NONE) {
    R.string.network_unknown
} else {
    type.labelRes()
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1220)
@Composable
private fun HistoryContentPreview() {
    NetraTheme(darkTheme = true) {
        HistoryContent(
            state = HistoryUiState(
                isLoading = false,
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
            onDelete = {},
            onClearAll = {},
            onStartTest = {},
        )
    }
}
