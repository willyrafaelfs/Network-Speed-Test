package io.github.willyrafaelfs.speedtest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.willyrafaelfs.speedtest.domain.model.ThemeMode
import io.github.willyrafaelfs.speedtest.presentation.MainActivityViewModel
import io.github.willyrafaelfs.speedtest.presentation.navigation.NetraApp
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Wajib sebelum super.onCreate: mengganti tema splash dengan tema aplikasi.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Tahan splash sampai preferensi terbaca, agar tidak ada kilatan tema yang salah.
        splashScreen.setKeepOnScreenCondition { viewModel.settings.value == null }
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val darkTheme = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Ikon status/navigation bar mengikuti tema APLIKASI, bukan tema sistem, sehingga
            // tetap terbaca saat user memilih tema yang berbeda dari sistem.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            NetraTheme(darkTheme = darkTheme, dynamicColor = current.dynamicColor) {
                NetraApp()
            }
        }
    }

    private companion object {
        // Scrim bawaan androidx.activity untuk navigation bar 3 tombol.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
