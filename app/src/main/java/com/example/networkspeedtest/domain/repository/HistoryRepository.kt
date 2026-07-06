package com.example.networkspeedtest.domain.repository

import com.example.networkspeedtest.domain.model.SpeedTestResult
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak penyimpanan riwayat hasil test. Implementasi di data layer
 * memakai Room (tahap 5).
 */
interface HistoryRepository {

    /** Memancarkan seluruh riwayat, terbaru lebih dulu, dan ikut berubah otomatis. */
    fun observeResults(): Flow<List<SpeedTestResult>>

    /** Menyimpan satu hasil test dan mengembalikan id barunya. */
    suspend fun saveResult(result: SpeedTestResult): Long

    /** Menghapus satu hasil berdasarkan id. */
    suspend fun deleteResult(id: Long)

    /** Menghapus seluruh riwayat. */
    suspend fun clearAll()
}
