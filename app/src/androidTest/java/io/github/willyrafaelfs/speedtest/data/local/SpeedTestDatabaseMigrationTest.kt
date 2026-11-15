package io.github.willyrafaelfs.speedtest.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Memastikan migrasi skema Room tidak menghilangkan riwayat yang sudah tersimpan.
 * Skema tiap versi dibaca dari `app/schemas/` (diekspor oleh Room Gradle plugin).
 */
@RunWith(AndroidJUnit4::class)
class SpeedTestDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SpeedTestDatabase::class.java,
    )

    @Test
    fun migrasi_1_ke_2_mempertahankan_data_dan_menambah_kolom_latency_kosong() {
        helper.createDatabase(TEST_DB, 1).use { it.insertV1Result() }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        migrated.query(
            "SELECT downloadMbps, networkName, downloadLatencyMs, uploadLatencyMs FROM speed_test_results",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(95.4, cursor.getDouble(0), DELTA)
            assertEquals("Rumah", cursor.getString(1))
            assertTrue("downloadLatencyMs harus NULL untuk hasil lama", cursor.isNull(2))
            assertTrue("uploadLatencyMs harus NULL untuk hasil lama", cursor.isNull(3))
        }
    }

    @Test
    fun migrasi_2_ke_3_menambah_tabel_sampel_dengan_cascade_delete() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO speed_test_results
                    (id, timestamp, pingMs, jitterMs, downloadMbps, uploadMbps, networkType, networkName,
                     downloadLatencyMs, uploadLatencyMs)
                VALUES (7, 1700000000000, 18.0, 2.0, 120.5, 30.2, 'CELLULAR', NULL, 64.0, 40.0)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        migrated.query("SELECT uploadLatencyMs FROM speed_test_results WHERE id = 7").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(40.0, cursor.getDouble(0), DELTA)
        }
        // Tabel baru bisa diisi, dan sampel ikut terhapus saat hasilnya dihapus.
        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL(
            "INSERT INTO speed_test_samples (resultId, phase, progress, mbps) VALUES (7, 'DOWNLOAD', 0.5, 118.0)",
        )
        assertEquals(1, migrated.countSamples())
        migrated.execSQL("DELETE FROM speed_test_results WHERE id = 7")
        assertEquals(0, migrated.countSamples())
    }

    @Test
    fun migrasi_berantai_1_ke_3_mempertahankan_data() {
        helper.createDatabase(TEST_DB, 1).use { it.insertV1Result() }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        migrated.query("SELECT COUNT(*) FROM speed_test_results").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.insertV1Result() = execSQL(
        """
        INSERT INTO speed_test_results
            (id, timestamp, pingMs, jitterMs, downloadMbps, uploadMbps, networkType, networkName)
        VALUES (1, 1700000000000, 21.5, 3.2, 95.4, 40.1, 'WIFI', 'Rumah')
        """.trimIndent(),
    )

    private fun SupportSQLiteDatabase.countSamples(): Int =
        query("SELECT COUNT(*) FROM speed_test_samples").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val DELTA = 1e-9
    }
}
