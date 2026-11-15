package io.github.willyrafaelfs.speedtest.domain.model

/**
 * Satu titik grafik throughput.
 *
 * @param progress posisi di dalam fase (0f..1f), menjadi sumbu X.
 * @param mbps kecepatan sesaat pada titik tersebut.
 */
data class TracePoint(val progress: Float, val mbps: Float)
