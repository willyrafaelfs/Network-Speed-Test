package io.github.willyrafaelfs.speedtest.presentation.history

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestResult

/**
 * State layar riwayat.
 *
 * @param results daftar hasil test (terbaru lebih dulu).
 * @param isLoading true selama data pertama belum termuat dari Room.
 */
data class HistoryUiState(
    val results: List<SpeedTestResult> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && results.isEmpty()
}
