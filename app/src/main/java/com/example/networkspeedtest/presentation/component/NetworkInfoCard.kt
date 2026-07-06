package com.example.networkspeedtest.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.networkspeedtest.domain.model.NetworkInfo
import com.example.networkspeedtest.domain.model.NetworkType

/**
 * Kartu info koneksi: jenis jaringan, nama (SSID/operator), dan IP publik.
 * Bila terhubung WiFi tapi nama belum bisa dibaca karena izin lokasi belum
 * diberikan, menampilkan ajakan meminta izin.
 */
@Composable
fun NetworkInfoCard(
    networkInfo: NetworkInfo,
    canReadWifiName: Boolean,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            InfoRow(label = "Koneksi", value = typeLabel(networkInfo))
            networkInfo.networkName?.let { name ->
                InfoRow(label = if (networkInfo.type == NetworkType.WIFI) "SSID" else "Operator", value = name)
            }
            InfoRow(label = "IP Publik", value = networkInfo.publicIp ?: "—")

            val needLocationForSsid = networkInfo.type == NetworkType.WIFI &&
                networkInfo.networkName == null &&
                !canReadWifiName
            if (needLocationForSsid) {
                Text(
                    text = "Beri izin lokasi untuk menampilkan nama WiFi (SSID).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRequestLocationPermission) {
                    Text("Izinkan Lokasi")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun typeLabel(info: NetworkInfo): String = when (info.type) {
    NetworkType.WIFI -> "WiFi"
    NetworkType.CELLULAR -> "Seluler"
    NetworkType.ETHERNET -> "Ethernet"
    NetworkType.VPN -> "VPN"
    NetworkType.OTHER -> "Lainnya"
    NetworkType.NONE -> if (info.isConnected) "Terhubung" else "Tidak terhubung"
}
