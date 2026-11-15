package io.github.willyrafaelfs.speedtest.data.repository

import io.github.willyrafaelfs.speedtest.data.local.dao.SpeedTestResultDao
import io.github.willyrafaelfs.speedtest.data.local.entity.toDomain
import io.github.willyrafaelfs.speedtest.data.local.entity.toEntity
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
