package io.github.willyrafaelfs.speedtest.presentation.component

import io.github.willyrafaelfs.speedtest.domain.model.BufferbloatGrade
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult

/** Metrik satu hasil test; null berarti belum/tidak terukur. Kecepatan selalu dalam Mbps. */
data class ResultMetrics(
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadLatencyMs: Double? = null,
    val uploadLatencyMs: Double? = null,
) {
    val bufferbloatGrade: BufferbloatGrade?
        get() = pingMs?.let { BufferbloatGrade.of(it, downloadLatencyMs, uploadLatencyMs) }
}

fun SpeedTestResult.toMetrics() = ResultMetrics(
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    pingMs = pingMs,
    jitterMs = jitterMs,
    downloadLatencyMs = downloadLatencyMs,
    uploadLatencyMs = uploadLatencyMs,
)
