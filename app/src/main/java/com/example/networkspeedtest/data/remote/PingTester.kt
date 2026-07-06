package com.example.networkspeedtest.data.remote

import com.example.networkspeedtest.domain.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * Mengukur latency & jitter dengan melakukan beberapa kali request HTTP ringan
 * ke [SpeedTestConfig.pingUrl] (endpoint 0 byte) dan mencatat waktu round-trip
 * tiap request.
 *
 * ICMP ping butuh akses root, maka RTT HTTP dipakai sebagai proxy latency yang wajar.
 */
class PingTester @Inject constructor(
    private val client: OkHttpClient,
    private val config: SpeedTestConfig,
) {
    suspend fun measure(): PingResult = withContext(Dispatchers.IO) {
        // Warm-up: buka koneksi + TLS handshake lebih dulu agar biaya sekali-pakai
        // itu tidak ikut terhitung sebagai latency.
        singleRequestMillis()

        val latencies = ArrayList<Double>(config.pingCount)
        repeat(config.pingCount) {
            coroutineContext.ensureActive() // hormati pembatalan di antara ping
            singleRequestMillis()?.let { latencies += it }
        }
        if (latencies.isEmpty()) throw IOException("Ping gagal: tidak ada respons dari server")

        val avg = latencies.average()
        // Jitter = rata-rata selisih absolut antar-latency berurutan (mean deviation).
        val jitter = if (latencies.size < 2) {
            0.0
        } else {
            latencies.zipWithNext { a, b -> abs(b - a) }.average()
        }
        PingResult(avgLatencyMs = avg, jitterMs = jitter)
    }

    /** Waktu round-trip satu request dalam ms, atau null bila gagal. */
    private fun singleRequestMillis(): Double? {
        val request = Request.Builder().url(config.pingUrl).build()
        val start = System.nanoTime()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.bytes() // konsumsi body sampai habis → RTT lengkap
                (System.nanoTime() - start) / NANOS_PER_MILLI
            }
        } catch (e: IOException) {
            null
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000.0
    }
}
