package com.example.networkspeedtest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.networkspeedtest.data.local.dao.SpeedTestResultDao
import com.example.networkspeedtest.data.local.entity.SpeedTestResultEntity

@Database(
    entities = [SpeedTestResultEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SpeedTestDatabase : RoomDatabase() {
    abstract fun speedTestResultDao(): SpeedTestResultDao

    companion object {
        const val NAME = "speed_test.db"
    }
}
