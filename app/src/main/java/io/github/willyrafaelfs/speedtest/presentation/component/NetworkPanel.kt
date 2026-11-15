package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.presentation.format.typeLabelRes
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/**
 * Info koneksi: jenis jaringan, nama Wi-Fi/operator, dan IP publik. Bila nama Wi-Fi
 * tersembunyi karena izin lokasi, panel menjelaskan sebabnya dan menawarkan langkah
 * yang sesuai dengan [locationAccess].
 */
@Composable
fun NetworkPanel(
    networkInfo: NetworkInfo,
    locationAccess: LocationAccess,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InstrumentPanel(modifier = modifier) {
        InfoRow(stringResource(R.string.network_connection), stringResource(networkInfo.typeLabelRes()))
        networkInfo.networkName?.let { name ->
            val label = if (networkInfo.type ==
                NetworkType.WIFI
            ) {
                R.string.network_wifi_name
            } else {
                R.string.network_carrier
            }
            InfoRow(stringResource(label), name)
        }
        InfoRow(
            stringResource(R.string.network_public_ip),
            networkInfo.publicIp ?: stringResource(R.string.value_missing),
        )

        val wifiNameHidden = networkInfo.type == NetworkType.WIFI &&
            networkInfo.networkName == null &&
            locationAccess != LocationAccess.GRANTED
        if (wifiNameHidden) {
            ReadoutDivider()
            val blocked = locationAccess == LocationAccess.BLOCKED
            Text(
                text = stringResource(if (blocked) R.string.permission_denied_hint else R.string.permission_ssid_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = if (blocked) onOpenSettings else onRequestLocation,
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .fillMaxWidth(),
            ) {
                Text(stringResource(if (blocked) R.string.permission_open_settings else R.string.permission_allow))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
