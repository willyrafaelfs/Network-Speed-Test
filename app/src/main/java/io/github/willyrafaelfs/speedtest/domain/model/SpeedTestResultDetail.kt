package io.github.willyrafaelfs.speedtest.domain.model

/**
 * Satu hasil test beserta jejak kecepatannya, untuk layar detail.
 * Hasil yang tersimpan sebelum jejak direkam punya daftar kosong.
 */
data class SpeedTestResultDetail(
    val result: SpeedTestResult,
    val downloadTrace: List<TracePoint>,
    val uploadTrace: List<TracePoint>,
)
