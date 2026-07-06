package com.example.networkspeedtest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.networkspeedtest.data.local.entity.SpeedTestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestResultDao {

    /** Riwayat terbaru lebih dulu; Flow otomatis emit ulang saat data berubah. */
    @Query("SELECT * FROM speed_test_results ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SpeedTestResultEntity>>

    @Insert
    suspend fun insert(entity: SpeedTestResultEntity): Long

    @Query("DELETE FROM speed_test_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM speed_test_results")
    suspend fun clearAll()
}
