package io.github.willyrafaelfs.speedtest.data.repository

import io.github.willyrafaelfs.speedtest.data.local.dao.DailyStatsRow
import io.github.willyrafaelfs.speedtest.data.local.dao.SpeedTestResultDao
import io.github.willyrafaelfs.speedtest.data.local.entity.SpeedTestSampleEntity
import io.github.willyrafaelfs.speedtest.data.local.entity.toDomain
import io.github.willyrafaelfs.speedtest.data.local.entity.toEntity
import io.github.willyrafaelfs.speedtest.data.local.entity.toSampleEntities
import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
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

    override fun observeResults(networkType: NetworkType?): Flow<List<SpeedTestResult>> =
        dao.observeAll(networkType?.name).map { entities -> entities.map { it.toDomain() } }

    override fun observeResult(id: Long): Flow<SpeedTestResultDetail?> =
        dao.observeWithSamples(id).map { it?.toDomain() }

    override fun observeDailyStats(networkType: NetworkType?): Flow<List<DailyStats>> =
        dao.observeDailyStats(networkType?.name).map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveResult(
        result: SpeedTestResult,
        downloadTrace: List<TracePoint>,
        uploadTrace: List<TracePoint>,
    ): Long = dao.insertWithSamples(result.toEntity()) { id ->
        downloadTrace.toSampleEntities(id, SpeedTestSampleEntity.PHASE_DOWNLOAD) +
            uploadTrace.toSampleEntities(id, SpeedTestSampleEntity.PHASE_UPLOAD)
    }

    override suspend fun deleteResult(id: Long) = dao.deleteById(id)

    override suspend fun clearAll() = dao.clearAll()
}

private fun DailyStatsRow.toDomain() = DailyStats(day, avgDownloadMbps, avgUploadMbps, avgPingMs, testCount)
