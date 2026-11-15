package io.github.willyrafaelfs.speedtest.testing

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.willyrafaelfs.speedtest.data.di.DatabaseModule
import io.github.willyrafaelfs.speedtest.data.di.RepositoryModule
import io.github.willyrafaelfs.speedtest.data.local.SpeedTestDatabase
import io.github.willyrafaelfs.speedtest.data.local.dao.SpeedTestResultDao
import io.github.willyrafaelfs.speedtest.data.repository.HistoryRepositoryImpl
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import io.github.willyrafaelfs.speedtest.domain.repository.NetworkInfoRepository
import io.github.willyrafaelfs.speedtest.domain.repository.SettingsRepository
import io.github.willyrafaelfs.speedtest.domain.repository.SpeedTestRepository
import javax.inject.Singleton

/**
 * Pengganti modul produksi untuk test UI berbasis Hilt: engine & info jaringan palsu
 * (tanpa internet), Settings in-memory, dan riwayat sungguhan di atas Room in-memory.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
object FakeRepositoryModule {

    @Provides
    @Singleton
    fun speedTestRepository(): SpeedTestRepository = FakeSpeedTestRepository()

    @Provides
    @Singleton
    fun networkInfoRepository(): NetworkInfoRepository = FakeNetworkInfoRepository()

    @Provides
    @Singleton
    fun settingsRepository(): SettingsRepository = FakeSettingsRepository()

    @Provides
    @Singleton
    fun historyRepository(dao: SpeedTestResultDao): HistoryRepository = HistoryRepositoryImpl(dao)
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object InMemoryDatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): SpeedTestDatabase =
        Room.inMemoryDatabaseBuilder(context, SpeedTestDatabase::class.java).build()

    @Provides
    fun dao(database: SpeedTestDatabase): SpeedTestResultDao = database.speedTestResultDao()
}
