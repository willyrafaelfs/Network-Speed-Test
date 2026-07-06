package com.example.networkspeedtest.data.di

import android.content.Context
import androidx.room.Room
import com.example.networkspeedtest.data.local.SpeedTestDatabase
import com.example.networkspeedtest.data.local.dao.SpeedTestResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Menyediakan instance Room database dan DAO-nya. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpeedTestDatabase =
        Room.databaseBuilder(
            context,
            SpeedTestDatabase::class.java,
            SpeedTestDatabase.NAME,
        ).build()

    @Provides
    fun provideSpeedTestResultDao(database: SpeedTestDatabase): SpeedTestResultDao =
        database.speedTestResultDao()
}
