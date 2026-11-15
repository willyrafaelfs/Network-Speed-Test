package io.github.willyrafaelfs.speedtest.testing

import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult

/** Contoh hasil test untuk dipakai bersama di berbagai test. */
fun testResult(
    id: Long = 0,
    timestamp: Long = 1_763_200_000_000,
    downloadMbps: Double = 100.0,
    uploadMbps: Double = 20.0,
    pingMs: Double = 20.0,
    networkType: NetworkType = NetworkType.WIFI,
) = SpeedTestResult(
    id = id,
    timestamp = timestamp,
    pingMs = pingMs,
    jitterMs = 2.0,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    networkType = networkType,
    networkName = "Lab",
    downloadLatencyMs = 60.0,
    uploadLatencyMs = 45.0,
)
