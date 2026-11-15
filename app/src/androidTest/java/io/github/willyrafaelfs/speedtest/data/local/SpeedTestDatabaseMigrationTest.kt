package io.github.willyrafaelfs.speedtest.data.local

import androidx.room.testing.MigrationTestHelper
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
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO speed_test_results
                    (id, timestamp, pingMs, jitterMs, downloadMbps, uploadMbps, networkType, networkName)
                VALUES (1, 1700000000000, 21.5, 3.2, 95.4, 40.1, 'WIFI', 'Rumah')
                """.trimIndent(),
            )
        }

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

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val DELTA = 1e-9
    }
}
