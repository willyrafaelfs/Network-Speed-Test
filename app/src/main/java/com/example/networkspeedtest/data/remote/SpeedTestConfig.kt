package com.example.networkspeedtest.data.remote

/**
 * Parameter untuk engine speed test. Memakai endpoint publik Cloudflare yang
 * lazim dipakai untuk pengukuran (menghasilkan/menerima byte dalam jumlah sembarang).
 *
 * @param downloadUrl endpoint yang mengembalikan sejumlah besar byte.
 * @param uploadUrl endpoint yang menerima POST besar (body dibuang server).
 * @param pingUrl endpoint ringan untuk mengukur latency (dipakai tahap 4).
 * @param publicIpUrl endpoint yang mengembalikan IP publik sebagai teks polos.
 * @param testDurationMillis lama tiap fase download/upload berjalan.
 * @param sampleIntervalMillis jarak antar emisi sampel real-time ke UI.
 * @param pingCount jumlah percobaan ping untuk menghitung jitter (tahap 4).
 */
data class SpeedTestConfig(
    // Maks ~90 MB per request di endpoint Cloudflare (di atas itu HTTP 403);
    // engine mengulang request sampai batas waktu, jadi 50 MB sudah cukup.
    val downloadUrl: String = "https://speed.cloudflare.com/__down?bytes=50000000", // 50 MB
    val uploadUrl: String = "https://speed.cloudflare.com/__up",
    val pingUrl: String = "https://speed.cloudflare.com/__down?bytes=0",
    val publicIpUrl: String = "https://api.ipify.org",
    val testDurationMillis: Long = 10_000L,
    val sampleIntervalMillis: Long = 300L,
    val pingCount: Int = 10,
)
