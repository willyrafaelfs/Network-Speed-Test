package io.github.willyrafaelfs.speedtest.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [SettingsRepository] berbasis DataStore Preferences.
 *
 * Nilai yang tidak dikenal (mis. ditulis versi aplikasi lain) jatuh ke default, sehingga
 * file preferensi yang rusak atau usang tidak pernah membuat aplikasi crash.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            // File korup/tidak terbaca: pakai default, tapi jangan telan error jenis lain.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.DURATION] = updated.testDurationSeconds
            prefs[Keys.STREAMS] = updated.streamCount
            prefs[Keys.UNIT] = updated.speedUnit.name
            prefs[Keys.THEME] = updated.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = updated.dynamicColor
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            testDurationSeconds = this[Keys.DURATION]?.takeIf { it in AppSettings.DURATION_OPTIONS }
                ?: defaults.testDurationSeconds,
            streamCount = this[Keys.STREAMS]?.takeIf { it in AppSettings.STREAM_OPTIONS } ?: defaults.streamCount,
            speedUnit = enumOrDefault(this[Keys.UNIT], defaults.speedUnit),
            themeMode = enumOrDefault(this[Keys.THEME], defaults.themeMode),
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val DURATION = intPreferencesKey("test_duration_seconds")
        val STREAMS = intPreferencesKey("stream_count")
        val UNIT = stringPreferencesKey("speed_unit")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
