package io.github.willyrafaelfs.speedtest.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.willyrafaelfs.speedtest.data.repository.HistoryRepositoryImpl
import io.github.willyrafaelfs.speedtest.data.repository.NetworkInfoRepositoryImpl
import io.github.willyrafaelfs.speedtest.data.repository.SpeedTestRepositoryImpl
import io.github.willyrafaelfs.speedtest.data.settings.SettingsRepositoryImpl
import io.github.willyrafaelfs.speedtest.domain.repository.HistoryRepository
import io.github.willyrafaelfs.speedtest.domain.repository.NetworkInfoRepository
import io.github.willyrafaelfs.speedtest.domain.repository.SettingsRepository
import io.github.willyrafaelfs.speedtest.domain.repository.SpeedTestRepository
import javax.inject.Singleton

/** Mengikat interface repository (domain) ke implementasinya (data). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSpeedTestRepository(
        impl: SpeedTestRepositoryImpl,
    ): SpeedTestRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl,
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindNetworkInfoRepository(
        impl: NetworkInfoRepositoryImpl,
    ): NetworkInfoRepository
}
