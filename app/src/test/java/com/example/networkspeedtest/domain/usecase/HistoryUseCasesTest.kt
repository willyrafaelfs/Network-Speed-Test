package com.example.networkspeedtest.domain.usecase

import com.example.networkspeedtest.domain.model.NetworkType
import com.example.networkspeedtest.domain.model.SpeedTestResult
import com.example.networkspeedtest.domain.repository.HistoryRepository
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

        override fun observeResults(): Flow<List<SpeedTestResult>> = flowOf(stored.toList())

        override suspend fun saveResult(result: SpeedTestResult): Long {
            stored += result
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

        val id = useCase(sampleResult())

        assertEquals(1L, id)
        assertEquals(1, repository.stored.size)
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
