package io.github.willyrafaelfs.speedtest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.willyrafaelfs.speedtest.data.local.dao.SpeedTestResultDao
import io.github.willyrafaelfs.speedtest.data.local.entity.SpeedTestResultEntity

@Database(
    entities = [SpeedTestResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SpeedTestDatabase : RoomDatabase() {
    abstract fun speedTestResultDao(): SpeedTestResultDao

    companion object {
        const val NAME = "speed_test.db"
    }
}
