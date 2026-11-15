package io.github.willyrafaelfs.speedtest.domain.repository

import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** Penyimpanan preferensi user. */
interface SettingsRepository {

    /** Preferensi terkini; emit ulang setiap kali ada perubahan. */
    val settings: Flow<AppSettings>

    /** Mengubah preferensi secara atomik berdasarkan nilai saat ini. */
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
