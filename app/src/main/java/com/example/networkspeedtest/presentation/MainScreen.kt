package com.example.networkspeedtest.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.networkspeedtest.presentation.history.HistoryScreen
import com.example.networkspeedtest.presentation.speedtest.SpeedTestScreen

/**
 * Root aplikasi: dua tab teks (Test & Riwayat). Memakai PrimaryTabRow agar
 * tidak butuh library Navigation maupun dependency ikon untuk aplikasi 2 layar.
 * Tab yang dipilih di-remember lintas rotasi via rememberSaveable.
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Test", "Riwayat")

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> SpeedTestScreen()
                else -> HistoryScreen()
            }
        }
    }
}
