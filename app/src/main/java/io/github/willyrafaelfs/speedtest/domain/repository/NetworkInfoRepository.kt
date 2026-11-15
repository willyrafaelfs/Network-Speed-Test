package io.github.willyrafaelfs.speedtest.domain.repository

import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak untuk membaca informasi koneksi jaringan (jenis, SSID/operator, IP publik).
 * Implementasi di data layer memakai ConnectivityManager/WifiManager (tahap 9).
 */
interface NetworkInfoRepository {

    /**
     * Memancarkan [NetworkInfo] terkini dan setiap kali koneksi berubah
     * (tersambung/terputus, ganti WiFi ke seluler, dsb).
     */
    fun observeNetworkInfo(): Flow<NetworkInfo>
}
