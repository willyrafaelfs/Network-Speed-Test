package com.example.networkspeedtest.data.di

import com.example.networkspeedtest.data.repository.SpeedTestRepositoryImpl
import com.example.networkspeedtest.domain.repository.SpeedTestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mengikat interface repository (domain) ke implementasinya (data).
 * NetworkInfoRepository dan HistoryRepository akan ditambahkan di tahap 9 & 5.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSpeedTestRepository(
        impl: SpeedTestRepositoryImpl,
    ): SpeedTestRepository
}
