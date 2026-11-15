package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResultDetail
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryUseCasesTest {

    /** HistoryRepository palsu berbasis list in-memory. */
    private class FakeHistoryRepository(
        initial: List<SpeedTestResult> = emptyList(),
    ) : HistoryRepository {
        val stored = initial.toMutableList()
        var clearedCount = 0

        val savedTraces = mutableListOf<Pair<List<TracePoint>, List<TracePoint>>>()

        override fun observeResults(networkType: NetworkType?): Flow<List<SpeedTestResult>> =
            flowOf(stored.filter { networkType == null || it.networkType == networkType })

        override fun observeResult(id: Long): Flow<SpeedTestResultDetail?> =
            flowOf(stored.firstOrNull { it.id == id }?.let { SpeedTestResultDetail(it, emptyList(), emptyList()) })

        override fun observeDailyStats(networkType: NetworkType?): Flow<List<DailyStats>> =
            flowOf(List(40) { day -> DailyStats("2026-10-${day + 1}", 50.0, 10.0, 20.0, 1) })

        override suspend fun saveResult(
            result: SpeedTestResult,
            downloadTrace: List<TracePoint>,
            uploadTrace: List<TracePoint>,
        ): Long {
            stored += result
            savedTraces += downloadTrace to uploadTrace
            return stored.size.toLong()
        }

        override suspend fun deleteResult(id: Long) {
            stored.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            clearedCount++
            stored.clear()
        }
    }

    private fun sampleResult(id: Long = 0) = SpeedTestResult(
        id = id,
        timestamp = 1_000L,
        pingMs = 20.0,
        jitterMs = 3.0,
        downloadMbps = 50.0,
        uploadMbps = 25.0,
        networkType = NetworkType.WIFI,
        networkName = "MyWiFi",
    )

    @Test
    fun `SaveTestResultUseCase mendelegasikan ke repository dan mengembalikan id`() = runTest {
        val repository = FakeHistoryRepository()
        val useCase = SaveTestResultUseCase(repository)

        val trace = listOf(TracePoint(0.5f, 42f))
        val id = useCase(sampleResult(), downloadTrace = trace)

        assertEquals(1L, id)
        assertEquals(1, repository.stored.size)
        assertEquals(trace to emptyList<TracePoint>(), repository.savedTraces.single())
    }

    @Test
    fun `ObserveTestHistoryUseCase meneruskan filter jenis jaringan`() = runTest {
        val wifi = sampleResult(id = 1)
        val cellular = sampleResult(id = 2).copy(networkType = NetworkType.CELLULAR)
        val useCase = ObserveTestHistoryUseCase(FakeHistoryRepository(listOf(wifi, cellular)))

        assertEquals(listOf(cellular), useCase(NetworkType.CELLULAR).first())
    }

    @Test
    fun `ObserveDailyStatsUseCase hanya mengambil hari-hari terakhir`() = runTest {
        val useCase = ObserveDailyStatsUseCase(FakeHistoryRepository())

        val stats = useCase(maxDays = 7).first()

        assertEquals(7, stats.size)
        assertEquals("2026-10-40", stats.last().day)
    }

    @Test
    fun `ObserveTestHistoryUseCase meneruskan data dari repository`() = runTest {
        val results = listOf(sampleResult(id = 1), sampleResult(id = 2))
        val useCase = ObserveTestHistoryUseCase(FakeHistoryRepository(results))

        assertEquals(results, useCase().first())
    }

    @Test
    fun `DeleteTestResultUseCase menghapus item sesuai id`() = runTest {
        val repository = FakeHistoryRepository(listOf(sampleResult(id = 1), sampleResult(id = 2)))
        val useCase = DeleteTestResultUseCase(repository)

        useCase(1)

        assertEquals(listOf(2L), repository.stored.map { it.id })
    }

    @Test
    fun `ClearHistoryUseCase mengosongkan seluruh riwayat`() = runTest {
        val repository = FakeHistoryRepository(listOf(sampleResult(id = 1)))
        val useCase = ClearHistoryUseCase(repository)

        useCase()

        assertEquals(0, repository.stored.size)
        assertEquals(1, repository.clearedCount)
    }
}
