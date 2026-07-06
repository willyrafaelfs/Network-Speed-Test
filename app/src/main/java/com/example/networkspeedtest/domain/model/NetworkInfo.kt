package com.example.networkspeedtest.domain.model

/**
 * Informasi koneksi jaringan saat ini.
 *
 * @param type jenis jaringan (WiFi / seluler / dll).
 * @param networkName SSID untuk WiFi atau nama operator untuk seluler.
 *                     Bisa null bila izin lokasi belum diberikan (WiFi) atau tidak diketahui.
 * @param publicIp alamat IP publik hasil query ke layanan eksternal; null bila belum/gagal diambil.
 * @param isConnected true bila ada koneksi internet aktif.
 */
data class NetworkInfo(
    val type: NetworkType,
    val networkName: String?,
    val publicIp: String?,
    val isConnected: Boolean,
) {
    companion object {
        /** State awal ketika belum ada informasi jaringan yang dibaca. */
        val Unknown = NetworkInfo(
            type = NetworkType.NONE,
            networkName = null,
            publicIp = null,
            isConnected = false,
        )
    }
}
