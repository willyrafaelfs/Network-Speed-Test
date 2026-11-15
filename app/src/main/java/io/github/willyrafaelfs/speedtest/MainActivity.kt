package io.github.willyrafaelfs.speedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.willyrafaelfs.speedtest.presentation.navigation.NetraApp
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetraTheme {
                NetraApp()
            }
        }
    }
}
