package io.github.willyrafaelfs.speedtest.domain.model

/**
 * Nilai bufferbloat: seberapa besar latency naik ketika koneksi sedang dipakai penuh
 * (download/upload berjalan) dibanding saat idle.
 *
 * Ambang batas mengikuti skala yang dipakai Waveform Bufferbloat Test, sehingga
 * hasilnya bisa dibandingkan dengan alat tersebut.
 */
enum class BufferbloatGrade(
    /** Batas atas (inklusif) kenaikan latency dalam ms untuk grade ini. */
    val maxIncreaseMs: Double,
) {
    A_PLUS(maxIncreaseMs = 5.0),
    A(maxIncreaseMs = 30.0),
    B(maxIncreaseMs = 60.0),
    C(maxIncreaseMs = 200.0),
    D(maxIncreaseMs = 400.0),
    F(maxIncreaseMs = Double.POSITIVE_INFINITY),
    ;

    companion object {
        /** Grade untuk kenaikan latency [increaseMs]; nilai negatif dianggap tidak naik. */
        fun fromLatencyIncrease(increaseMs: Double): BufferbloatGrade =
            entries.first { increaseMs.coerceAtLeast(0.0) <= it.maxIncreaseMs }

        /**
         * Grade dari latency idle dan latency saat beban (download & upload).
         * Yang dipakai adalah fase dengan kenaikan terburuk. Null bila belum ada
         * satu pun pengukuran latency saat beban.
         */
        fun of(idleLatencyMs: Double, vararg loadedLatencyMs: Double?): BufferbloatGrade? {
            val worstLoaded = loadedLatencyMs.filterNotNull().maxOrNull() ?: return null
            return fromLatencyIncrease(worstLoaded - idleLatencyMs)
        }
    }
}
