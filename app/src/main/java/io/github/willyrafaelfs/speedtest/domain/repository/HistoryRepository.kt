package io.github.willyrafaelfs.speedtest.domain.repository

import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak penyimpanan riwayat hasil test. Parameter [NetworkType] bernilai null
 * berarti semua jenis jaringan.
 */
interface HistoryRepository {

    /** Riwayat (terbaru lebih dulu), ikut berubah otomatis saat data berubah. */
    fun observeResults(networkType: NetworkType? = null): Flow<List<SpeedTestResult>>

    /** Satu hasil beserta jejak kecepatannya; null bila sudah dihapus. */
    fun observeResult(id: Long): Flow<SpeedTestResultDetail?>

    /** Rata-rata per hari, urut dari hari terlama. */
    fun observeDailyStats(networkType: NetworkType? = null): Flow<List<DailyStats>>

    /** Menyimpan satu hasil test beserta jejaknya dan mengembalikan id barunya. */
    suspend fun saveResult(
        result: SpeedTestResult,
        downloadTrace: List<TracePoint> = emptyList(),
        uploadTrace: List<TracePoint> = emptyList(),
    ): Long

    /** Menghapus satu hasil (jejaknya ikut terhapus). */
    suspend fun deleteResult(id: Long)

    /** Menghapus seluruh riwayat. */
    suspend fun clearAll()
}
