package com.example.networkspeedtest.domain.model

/**
 * Hasil akhir satu kali test kecepatan jaringan. Model domain murni yang
 * juga menjadi acuan untuk disimpan ke Room (lihat tahap 5).
 *
 * @param id id lokal (0 berarti belum disimpan; akan diisi Room saat insert).
 * @param timestamp waktu test dijalankan (epoch millis).
 * @param pingMs rata-rata latency (ms).
 * @param jitterMs variasi latency antar-ping (ms).
 * @param downloadMbps kecepatan download (Megabit per detik).
 * @param uploadMbps kecepatan upload (Megabit per detik).
 * @param networkType jenis jaringan saat test dilakukan.
 * @param networkName SSID / nama operator saat test dilakukan.
 */
data class SpeedTestResult(
    val id: Long = 0,
    val timestamp: Long,
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val networkType: NetworkType,
    val networkName: String?,
)
