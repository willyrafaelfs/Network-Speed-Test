package io.github.willyrafaelfs.speedtest.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.willyrafaelfs.speedtest.BuildConfig
import io.github.willyrafaelfs.speedtest.data.remote.SpeedTestConfig
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

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
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Durasi total dikendalikan manual oleh engine, jadi call timeout dimatikan.
            .callTimeout(0, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
}

private const val CONNECT_TIMEOUT_SECONDS = 15L
private const val IO_TIMEOUT_SECONDS = 30L
