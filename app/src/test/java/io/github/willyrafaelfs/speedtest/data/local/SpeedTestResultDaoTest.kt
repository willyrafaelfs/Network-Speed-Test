package io.github.willyrafaelfs.speedtest.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.willyrafaelfs.speedtest.data.local.dao.SpeedTestResultDao
import io.github.willyrafaelfs.speedtest.data.local.entity.SpeedTestSampleEntity
import io.github.willyrafaelfs.speedtest.data.local.entity.toDomain
import io.github.willyrafaelfs.speedtest.data.local.entity.toEntity
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.testing.testResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Query Room terhadap database in-memory sungguhan (SQLite lewat Robolectric). */
@RunWith(AndroidJUnit4::class)
class SpeedTestResultDaoTest {

    private lateinit var database: SpeedTestDatabase
    private lateinit var dao: SpeedTestResultDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpeedTestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.speedTestResultDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `hasil dan jejaknya tersimpan bersama dan terbaca kembali per fase`() = runTest {
        val id = dao.insertWithSamples(testResult().toEntity()) { resultId ->
            listOf(
                SpeedTestSampleEntity(resultId = resultId, phase = "DOWNLOAD", progress = 0.6f, mbps = 90f),
                SpeedTestSampleEntity(resultId = resultId, phase = "DOWNLOAD", progress = 0.2f, mbps = 40f),
                SpeedTestSampleEntity(resultId = resultId, phase = "UPLOAD", progress = 0.5f, mbps = 20f),
            )
        }

        val detail = dao.observeWithSamples(id).first()!!.toDomain()

        assertEquals(id, detail.result.id)
        // Diurutkan berdasarkan progress, terlepas dari urutan insert.
        assertEquals(listOf(TracePoint(0.2f, 40f), TracePoint(0.6f, 90f)), detail.downloadTrace)
        assertEquals(listOf(TracePoint(0.5f, 20f)), detail.uploadTrace)
    }

    @Test
    fun `riwayat terbaru lebih dulu dan bisa difilter per jenis jaringan`() = runTest {
        dao.insert(testResult(timestamp = 1_000).toEntity())
        dao.insert(testResult(timestamp = 3_000, networkType = NetworkType.CELLULAR).toEntity())
        dao.insert(testResult(timestamp = 2_000).toEntity())

        assertEquals(listOf(3_000L, 2_000L, 1_000L), dao.observeAll().first().map { it.timestamp })
        assertEquals(listOf(2_000L, 1_000L), dao.observeAll("WIFI").first().map { it.timestamp })
        assertEquals(listOf(3_000L), dao.observeAll("CELLULAR").first().map { it.timestamp })
    }

    @Test
    fun `statistik harian merata-rata hasil per hari dan urut dari hari terlama`() = runTest {
        // Dua hasil di hari pertama dan satu di hari ketiga (siang UTC, jauh dari pergantian hari).
        dao.insert(testResult(timestamp = DAY_ONE, downloadMbps = 100.0, pingMs = 10.0).toEntity())
        dao.insert(testResult(timestamp = DAY_ONE + HOUR, downloadMbps = 50.0, pingMs = 30.0).toEntity())
        dao.insert(testResult(timestamp = DAY_ONE + 2 * DAY, downloadMbps = 80.0).toEntity())

        val stats = dao.observeDailyStats().first()

        assertEquals(2, stats.size)
        assertEquals(75.0, stats[0].avgDownloadMbps, DELTA)
        assertEquals(20.0, stats[0].avgPingMs, DELTA)
        assertEquals(2, stats[0].testCount)
        assertEquals(80.0, stats[1].avgDownloadMbps, DELTA)
        assertEquals(true, stats[0].day < stats[1].day)
    }

    @Test
    fun `menghapus hasil ikut menghapus sampelnya (cascade)`() = runTest {
        val id = dao.insertWithSamples(testResult().toEntity()) { resultId ->
            listOf(SpeedTestSampleEntity(resultId = resultId, phase = "DOWNLOAD", progress = 0.5f, mbps = 1f))
        }

        dao.deleteById(id)

        assertNull(dao.observeWithSamples(id).first())
        assertEquals(0, countSamples())
    }

    @Test
    fun `hapus semua mengosongkan hasil dan sampel`() = runTest {
        repeat(3) {
            dao.insertWithSamples(testResult().toEntity()) { resultId ->
                listOf(SpeedTestSampleEntity(resultId = resultId, phase = "UPLOAD", progress = 0.5f, mbps = 1f))
            }
        }

        dao.clearAll()

        assertEquals(0, dao.observeAll().first().size)
        assertEquals(0, countSamples())
    }

    private fun countSamples(): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM speed_test_samples").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val DAY_ONE = 1_763_121_600_000 // 14 November 2026, 12:00 UTC
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
        const val DELTA = 1e-9
    }
}
