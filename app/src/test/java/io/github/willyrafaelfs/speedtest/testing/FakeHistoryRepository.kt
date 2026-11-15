package io.github.willyrafaelfs.speedtest.testing

import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** HistoryRepository in-memory yang reaktif seperti Room. */
class FakeHistoryRepository(initial: List<SpeedTestResult> = emptyList()) : HistoryRepository {

    private val details = MutableStateFlow(initial.map { SpeedTestResultDetail(it, emptyList(), emptyList()) })
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    /** Bila true, saveResult gagal seperti disk penuh/DB terkunci. */
    var failOnSave = false

    val stored: List<SpeedTestResultDetail> get() = details.value

    override fun observeResults(networkType: NetworkType?): Flow<List<SpeedTestResult>> = details.map { list ->
        list.map { it.result }
            .filter { networkType == null || it.networkType == networkType }
            .sortedByDescending { it.timestamp }
    }

    override fun observeResult(id: Long): Flow<SpeedTestResultDetail?> =
        details.map { list -> list.firstOrNull { it.result.id == id } }

    override fun observeDailyStats(networkType: NetworkType?): Flow<List<DailyStats>> = observeResults(
        networkType,
    ).map { results ->
        results.groupBy { DAY_FORMAT.format(java.util.Date(it.timestamp)) }
            .toSortedMap()
            .map { (day, items) ->
                DailyStats(
                    day = day,
                    avgDownloadMbps = items.map { it.downloadMbps }.average(),
                    avgUploadMbps = items.map { it.uploadMbps }.average(),
                    avgPingMs = items.map { it.pingMs }.average(),
                    testCount = items.size,
                )
            }
    }

    override suspend fun saveResult(
        result: SpeedTestResult,
        downloadTrace: List<TracePoint>,
        uploadTrace: List<TracePoint>,
    ): Long {
        if (failOnSave) throw IOException("disk full")
        val id = nextId++
        details.update { it + SpeedTestResultDetail(result.copy(id = id), downloadTrace, uploadTrace) }
        return id
    }

    override suspend fun deleteResult(id: Long) = details.update { list -> list.filterNot { it.result.id == id } }

    override suspend fun clearAll() = details.update { emptyList() }

    private companion object {
        val DAY_FORMAT = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    }
}
