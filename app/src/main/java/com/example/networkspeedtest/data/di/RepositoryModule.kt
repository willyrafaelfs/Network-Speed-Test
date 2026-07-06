package com.example.networkspeedtest.data.di

import com.example.networkspeedtest.data.repository.HistoryRepositoryImpl
import com.example.networkspeedtest.data.repository.NetworkInfoRepositoryImpl
import com.example.networkspeedtest.data.repository.SpeedTestRepositoryImpl
import com.example.networkspeedtest.domain.repository.HistoryRepository
import com.example.networkspeedtest.domain.repository.NetworkInfoRepository
import com.example.networkspeedtest.domain.repository.SpeedTestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    abstract fun bindNetworkInfoRepository(
        impl: NetworkInfoRepositoryImpl,
    ): NetworkInfoRepository
}
