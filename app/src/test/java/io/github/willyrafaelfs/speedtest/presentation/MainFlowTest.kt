package io.github.willyrafaelfs.speedtest.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.willyrafaelfs.speedtest.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Alur utama end-to-end di dalam aplikasi sungguhan (Activity, Hilt, Navigation, Room),
 * dengan engine palsu sehingga hasilnya pasti: download 92,5 Mbps, upload 33 Mbps, ping 20 ms.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class MainFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun mulai_test_lalu_hasil_tersimpan_di_riwayat_dan_bisa_dibuka() {
        composeRule.onNodeWithText("Start test").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Test again"), TIMEOUT_MILLIS)

        // Hasil tampil di layar test.
        composeRule.onNodeWithContentDescription("Download: 92.5 Mbps", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Upload: 33.0 Mbps", useUnmergedTree = true).assertExists()

        // Hasil yang sama tersimpan di riwayat.
        composeRule.onNodeWithText("History").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("1 result"), TIMEOUT_MILLIS)
        composeRule.onNodeWithContentDescription("Download: 92.5 Mbps", useUnmergedTree = true).assertExists()

        // Membuka detail hasil.
        composeRule.onNodeWithContentDescription("Download: 92.5 Mbps", useUnmergedTree = true).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Share"), TIMEOUT_MILLIS)
        composeRule.onNodeWithText("Result").assertIsDisplayed()
    }

    @Test
    fun riwayat_kosong_mengajak_menjalankan_test() {
        composeRule.onNodeWithText("History").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("No results yet"), TIMEOUT_MILLIS)

        composeRule.onNodeWithText("Start test").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Ready to test"), TIMEOUT_MILLIS)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
