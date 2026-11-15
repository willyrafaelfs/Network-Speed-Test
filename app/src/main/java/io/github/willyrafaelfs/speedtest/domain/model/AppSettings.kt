package io.github.willyrafaelfs.speedtest.domain.model

/** Satuan tampilan kecepatan. Nilai selalu disimpan dalam Mbps; ini hanya soal tampilan. */
enum class SpeedUnit {
    /** Megabit per detik: satuan baku penyedia internet. */
    MBPS,

    /** Megabyte per detik (= Mbps / 8): lebih intuitif untuk ukuran file. */
    MEGABYTES_PER_SECOND,
}

/** Pilihan tema aplikasi. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Preferensi user. Nilai default dipilih agar hasil sebanding dengan alat ukur lain
 * (lihat ADR 001).
 *
 * @param testDurationSeconds lama tiap fase download/upload.
 * @param streamCount jumlah koneksi paralel untuk download/upload.
 * @param speedUnit satuan tampilan kecepatan.
 * @param themeMode tema terang/gelap atau mengikuti sistem.
 * @param dynamicColor warna Material dari wallpaper (Android 12+); warna kanal tetap.
 */
data class AppSettings(
    val testDurationSeconds: Int = DEFAULT_DURATION_SECONDS,
    val streamCount: Int = DEFAULT_STREAM_COUNT,
    val speedUnit: SpeedUnit = SpeedUnit.MBPS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
) {
    /** Opsi pengukuran yang diturunkan dari preferensi ini. */
    val measurementOptions: MeasurementOptions
        get() = MeasurementOptions(durationMillis = testDurationSeconds * MILLIS_PER_SECOND, streamCount = streamCount)

    companion object {
        const val DEFAULT_DURATION_SECONDS = 10
        const val DEFAULT_STREAM_COUNT = 4

        /** Pilihan yang ditawarkan di layar Settings. */
        val DURATION_OPTIONS = listOf(10, 15, 20)
        val STREAM_OPTIONS = listOf(1, 2, 4, 6, 8)

        internal const val MILLIS_PER_SECOND = 1_000L
    }
}

/** Parameter satu kali pengukuran download/upload. */
data class MeasurementOptions(
    val durationMillis: Long = AppSettings.DEFAULT_DURATION_SECONDS * AppSettings.MILLIS_PER_SECOND,
    val streamCount: Int = AppSettings.DEFAULT_STREAM_COUNT,
)
