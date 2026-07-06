package com.example.networkspeedtest.data.di

import com.example.networkspeedtest.BuildConfig
import com.example.networkspeedtest.data.remote.SpeedTestConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Menyediakan OkHttpClient dan konfigurasi test untuk seluruh aplikasi. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSpeedTestConfig(): SpeedTestConfig = SpeedTestConfig()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Hanya log baris request/response di debug; tidak pernah membaca body
            // besar (download/upload) sehingga tidak memengaruhi pengukuran.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Durasi total dikendalikan manual oleh engine, jadi call timeout dimatikan.
            .callTimeout(0, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
}
