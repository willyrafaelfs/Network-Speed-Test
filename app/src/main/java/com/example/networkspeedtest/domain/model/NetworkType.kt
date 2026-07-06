package com.example.networkspeedtest.domain.model

/**
 * Jenis koneksi jaringan aktif. Dipakai untuk menampilkan info koneksi dan
 * disimpan bersama riwayat hasil test.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    OTHER,
    NONE,
}
