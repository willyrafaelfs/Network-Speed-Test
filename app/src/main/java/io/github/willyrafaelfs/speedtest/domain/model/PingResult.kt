package io.github.willyrafaelfs.speedtest.domain.model

/**
 * Hasil pengukuran ping/jitter dari beberapa kali percobaan.
 *
 * @param latencyMs median latency seluruh sampel (ms); median dipakai agar satu ping yang
 *                  lambat (mis. retransmisi atau CPU sibuk) tidak menggeser hasil.
 * @param jitterMs rata-rata selisih absolut antar-sampel berurutan (ms).
 */
data class PingResult(
    val latencyMs: Double,
    val jitterMs: Double,
)
