package io.github.willyrafaelfs.speedtest.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Menguji SettingsRepositoryImpl terhadap DataStore sungguhan di folder sementara. */
class SettingsRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
        File(folder.root, "settings.preferences_pb")
    }
    private val repository = SettingsRepositoryImpl(dataStore)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `tanpa data tersimpan memakai nilai default`() = runTest {
        assertEquals(AppSettings(), repository.settings.first())
    }

    @Test
    fun `perubahan tersimpan dan terbaca kembali`() = runTest {
        repository.update {
            it.copy(
                testDurationSeconds = 20,
                streamCount = 8,
                speedUnit = SpeedUnit.MEGABYTES_PER_SECOND,
                themeMode = ThemeMode.DARK,
                dynamicColor = true,
            )
        }

        val expected = AppSettings(20, 8, SpeedUnit.MEGABYTES_PER_SECOND, ThemeMode.DARK, dynamicColor = true)
        assertEquals(expected, repository.settings.first())
    }

    @Test
    fun `nilai tidak dikenal jatuh ke default alih-alih crash`() = runTest {
        dataStore.edit {
            it[intPreferencesKey("test_duration_seconds")] = 999
            it[intPreferencesKey("stream_count")] = 3
            it[stringPreferencesKey("speed_unit")] = "GIGABITS"
            it[stringPreferencesKey("theme_mode")] = "SEPIA"
        }

        assertEquals(AppSettings(), repository.settings.first())
    }
}
