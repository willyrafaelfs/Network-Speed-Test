package com.example.networkspeedtest.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.networkspeedtest.presentation.history.HistoryScreen
import com.example.networkspeedtest.presentation.speedtest.SpeedTestScreen

/**
 * Root aplikasi. SATU Scaffold di level teratas membungkus seluruh navigasi
 * sehingga background (colorScheme.background) dan snackbar konsisten untuk
 * semua tab — tiap screen tidak lagi punya Scaffold/background sendiri.
 *
 * Navigasi memakai PrimaryTabRow (tab teks) agar tidak butuh library Navigation
 * maupun dependency ikon untuk aplikasi 2 layar.
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Test", "Riwayat")
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.statusBarsPadding(),
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                0 -> SpeedTestScreen(snackbarHostState = snackbarHostState)
                else -> HistoryScreen()
            }
        }
    }
}
