package com.example.networkspeedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.networkspeedtest.presentation.speedtest.SpeedTestScreen
import com.example.networkspeedtest.ui.theme.NetworkSpeedTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkSpeedTestTheme {
                SpeedTestScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
