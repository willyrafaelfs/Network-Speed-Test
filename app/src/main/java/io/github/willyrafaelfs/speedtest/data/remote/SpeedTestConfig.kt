package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.MeasurementOptions

/**
 * Parameter untuk engine speed test. Memakai endpoint publik Cloudflare yang
 * lazim dipakai untuk pengukuran (menghasilkan/menerima byte dalam jumlah sembarang).
 *
 * Alasan di balik nilai default dijelaskan di `docs/adr/001-measurement-methodology.md`.
 *
 * @param downloadUrl endpoint yang mengembalikan sejumlah besar byte.
 * @param uploadUrl endpoint yang menerima POST besar (body dibuang server).
 * @param pingUrl endpoint ringan (0 byte) untuk mengukur latency.
 * @param publicIpUrl endpoint yang mengembalikan IP publik sebagai teks polos.
 * @param testDurationMillis lama tiap fase download/upload berjalan.
 * @param sampleIntervalMillis jarak antar emisi sampel real-time ke UI.
 * @param pingCount jumlah percobaan ping idle untuk menghitung latency & jitter.
 * @param streamCount jumlah koneksi TCP paralel untuk download/upload.
 * @param rampUpMillis awal fase yang diabaikan dari hasil final (TCP slow start).
 * @param windowMillis lebar jendela untuk menghitung throughput per titik waktu.
 * @param resultPercentile persentil throughput per jendela yang dijadikan hasil final.
 * @param loadedPingIntervalMillis jarak antar ping selama download/upload berjalan.
 * @param uploadMinRequestBytes ukuran request upload pertama di tiap koneksi.
 * @param uploadMaxRequestBytes ukuran maksimum satu request upload.
 * @param uploadTargetRequestMillis ukuran request digandakan selama request selesai lebih cepat dari ini.
 */
data class SpeedTestConfig(
    // Maks ~90 MB per request di endpoint Cloudflare (di atas itu HTTP 403);
    // engine mengulang request sampai batas waktu, jadi 50 MB sudah cukup.
    val downloadUrl: String = "https://speed.cloudflare.com/__down?bytes=50000000", // 50 MB
    val uploadUrl: String = "https://speed.cloudflare.com/__up",
    val pingUrl: String = "https://speed.cloudflare.com/__down?bytes=0",
    val publicIpUrl: String = "https://api.ipify.org",
    val testDurationMillis: Long = 10_000L,
    val sampleIntervalMillis: Long = 250L,
    val pingCount: Int = 10,
    val streamCount: Int = 4,
    val rampUpMillis: Long = 2_000L,
    val windowMillis: Long = 1_000L,
    val resultPercentile: Double = 0.9,
    val loadedPingIntervalMillis: Long = 500L,
    val uploadMinRequestBytes: Long = DEFAULT_UPLOAD_MIN_REQUEST_BYTES,
    val uploadMaxRequestBytes: Long = DEFAULT_UPLOAD_MAX_REQUEST_BYTES,
    val uploadTargetRequestMillis: Long = 1_000L,
) {
    init {
        require(streamCount >= 1) { "streamCount minimal 1" }
        require(rampUpMillis in 0 until testDurationMillis) { "rampUpMillis harus lebih pendek dari durasi test" }
        require(resultPercentile in 0.0..1.0) { "resultPercentile harus di rentang 0..1" }
        require(uploadMinRequestBytes in 1..uploadMaxRequestBytes) { "ukuran request upload tidak valid" }
    }
}

private const val DEFAULT_UPLOAD_MIN_REQUEST_BYTES = 64L * 1024 // 64 KiB
private const val DEFAULT_UPLOAD_MAX_REQUEST_BYTES = 16L * 1024 * 1024 // 16 MiB

/** Konfigurasi dengan durasi & jumlah koneksi dari preferensi user. */
internal fun SpeedTestConfig.withOptions(options: MeasurementOptions): SpeedTestConfig =
    copy(testDurationMillis = options.durationMillis, streamCount = options.streamCount)
