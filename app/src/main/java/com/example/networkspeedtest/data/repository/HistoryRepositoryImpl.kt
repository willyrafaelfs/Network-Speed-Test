package com.example.networkspeedtest.data.repository

import com.example.networkspeedtest.data.local.dao.SpeedTestResultDao
import com.example.networkspeedtest.data.local.entity.toDomain
import com.example.networkspeedtest.data.local.entity.toEntity
import com.example.networkspeedtest.domain.model.SpeedTestResult
import com.example.networkspeedtest.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementasi [HistoryRepository] berbasis Room. Memetakan entity <-> domain
 * agar lapisan atas tidak pernah menyentuh tipe milik Room.
 */
class HistoryRepositoryImpl @Inject constructor(
    private val dao: SpeedTestResultDao,
) : HistoryRepository {

    override fun observeResults(): Flow<List<SpeedTestResult>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveResult(result: SpeedTestResult): Long =
        dao.insert(result.toEntity())

    override suspend fun deleteResult(id: Long) = dao.deleteById(id)

    override suspend fun clearAll() = dao.clearAll()
}
