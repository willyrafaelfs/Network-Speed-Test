package com.example.networkspeedtest.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.networkspeedtest.domain.model.NetworkType
import com.example.networkspeedtest.domain.model.SpeedTestResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onDelete = viewModel::deleteResult,
        onClearAll = viewModel::clearAll,
        modifier = modifier,
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    when {
        state.isLoading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.isEmpty -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Belum ada riwayat.\nJalankan test untuk melihat hasilnya di sini.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Riwayat (${state.results.size})",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        TextButton(onClick = { showClearDialog = true }) {
                            Text("Hapus Semua")
                        }
                    }
                }
                items(state.results, key = { it.id }) { result ->
                    HistoryItemCard(result = result, onDelete = { onDelete(result.id) })
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus semua riwayat?") },
            text = { Text("Seluruh hasil test yang tersimpan akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    showClearDialog = false
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun HistoryItemCard(
    result: SpeedTestResult,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTimestamp(result.timestamp),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = networkLabel(result),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricInline("Ping", formatMs(result.pingMs), "ms")
                MetricInline("Jitter", formatMs(result.jitterMs), "ms")
                MetricInline("Down", formatSpeed(result.downloadMbps), "Mbps")
                MetricInline("Up", formatSpeed(result.uploadMbps), "Mbps")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun MetricInline(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val dateFormatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())

private fun formatTimestamp(timestamp: Long): String = dateFormatter.format(Date(timestamp))

private fun networkLabel(result: SpeedTestResult): String = result.networkName ?: when (result.networkType) {
    NetworkType.WIFI -> "WiFi"
    NetworkType.CELLULAR -> "Seluler"
    NetworkType.ETHERNET -> "Ethernet"
    NetworkType.VPN -> "VPN"
    NetworkType.OTHER -> "Lainnya"
    NetworkType.NONE -> "Tidak diketahui"
}

private fun formatSpeed(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun formatMs(value: Double): String = String.format(Locale.US, "%.0f", value)
