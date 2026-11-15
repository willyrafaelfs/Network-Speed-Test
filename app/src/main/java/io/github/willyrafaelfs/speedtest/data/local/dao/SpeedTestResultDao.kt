package io.github.willyrafaelfs.speedtest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.willyrafaelfs.speedtest.data.local.entity.ResultWithSamples
import io.github.willyrafaelfs.speedtest.data.local.entity.SpeedTestResultEntity
import io.github.willyrafaelfs.speedtest.data.local.entity.SpeedTestSampleEntity
import kotlinx.coroutines.flow.Flow

/** Baris hasil query agregat harian. */
data class DailyStatsRow(
    val day: String,
    val avgDownloadMbps: Double,
    val avgUploadMbps: Double,
    val avgPingMs: Double,
    val testCount: Int,
)

@Dao
interface SpeedTestResultDao {

    /**
     * Riwayat terbaru lebih dulu; Flow otomatis emit ulang saat data berubah.
     * [networkType] null berarti semua jenis jaringan.
     */
    @Query(
        """
        SELECT * FROM speed_test_results
        WHERE (:networkType IS NULL OR networkType = :networkType)
        ORDER BY timestamp DESC
        """,
    )
    fun observeAll(networkType: String? = null): Flow<List<SpeedTestResultEntity>>

    @Transaction
    @Query("SELECT * FROM speed_test_results WHERE id = :id")
    fun observeWithSamples(id: Long): Flow<ResultWithSamples?>

    /** Rata-rata per hari menurut zona waktu perangkat, urut dari hari terlama. */
    @Query(
        """
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS day,
               AVG(downloadMbps) AS avgDownloadMbps,
               AVG(uploadMbps) AS avgUploadMbps,
               AVG(pingMs) AS avgPingMs,
               COUNT(*) AS testCount
        FROM speed_test_results
        WHERE (:networkType IS NULL OR networkType = :networkType)
        GROUP BY day
        ORDER BY day ASC
        """,
    )
    fun observeDailyStats(networkType: String? = null): Flow<List<DailyStatsRow>>

    @Insert
    suspend fun insert(entity: SpeedTestResultEntity): Long

    @Insert
    suspend fun insertSamples(samples: List<SpeedTestSampleEntity>)

    /** Menyimpan hasil dan sampelnya secara atomik: tidak pernah ada hasil tanpa sampel separuh jalan. */
    @Transaction
    suspend fun insertWithSamples(
        entity: SpeedTestResultEntity,
        samplesFor: (resultId: Long) -> List<SpeedTestSampleEntity>,
    ): Long {
        val id = insert(entity)
        insertSamples(samplesFor(id))
        return id
    }

    @Query("DELETE FROM speed_test_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM speed_test_results")
    suspend fun clearAll()
}
