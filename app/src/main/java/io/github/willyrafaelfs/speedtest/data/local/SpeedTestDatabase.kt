package io.github.willyrafaelfs.speedtest.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.willyrafaelfs.speedtest.data.local.dao.SpeedTestResultDao
import io.github.willyrafaelfs.speedtest.data.local.entity.SpeedTestResultEntity
import io.github.willyrafaelfs.speedtest.data.local.entity.SpeedTestSampleEntity

@Database(
    entities = [SpeedTestResultEntity::class, SpeedTestSampleEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v2: kolom latency saat beban (downloadLatencyMs, uploadLatencyMs).
        AutoMigration(from = 1, to = 2),
        // v3: tabel speed_test_samples untuk jejak kecepatan per hasil.
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class SpeedTestDatabase : RoomDatabase() {
    abstract fun speedTestResultDao(): SpeedTestResultDao

    companion object {
        const val NAME = "speed_test.db"
    }
}
