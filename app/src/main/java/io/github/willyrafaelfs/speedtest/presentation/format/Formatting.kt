package io.github.willyrafaelfs.speedtest.presentation.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.BufferbloatGrade
import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import io.github.willyrafaelfs.speedtest.domain.model.NetworkType
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import java.text.NumberFormat
import java.util.Locale

/** Locale UI saat ini; dibaca dari Configuration agar ikut berubah saat bahasa diganti. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Angka desimal sesuai locale (mis. "87.4" di EN, "87,4" di ID). */
fun formatDecimal(value: Double, fractionDigits: Int, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }.format(value)

/** Kecepatan dengan 1 angka desimal, atau tanda kosong bila belum terukur. */
@Composable
@ReadOnlyComposable
fun formatMbps(value: Double?): String =
    value?.let { formatDecimal(it, 1, currentLocale()) } ?: stringResource(R.string.value_missing)

/** Latency dalam ms tanpa desimal, atau tanda kosong bila belum terukur. */
@Composable
@ReadOnlyComposable
fun formatMs(value: Double?): String =
    value?.let { formatDecimal(it, 0, currentLocale()) } ?: stringResource(R.string.value_missing)

@StringRes
fun SpeedTestPhase.labelRes(): Int = when (this) {
    SpeedTestPhase.IDLE -> R.string.phase_idle
    SpeedTestPhase.PING -> R.string.phase_ping
    SpeedTestPhase.DOWNLOAD -> R.string.phase_download
    SpeedTestPhase.UPLOAD -> R.string.phase_upload
    SpeedTestPhase.FINISHED -> R.string.phase_finished
}

@StringRes
fun NetworkType.labelRes(isConnected: Boolean = true): Int = when (this) {
    NetworkType.WIFI -> R.string.network_type_wifi
    NetworkType.CELLULAR -> R.string.network_type_cellular
    NetworkType.ETHERNET -> R.string.network_type_ethernet
    NetworkType.VPN -> R.string.network_type_vpn
    NetworkType.OTHER -> R.string.network_type_other
    NetworkType.NONE -> if (isConnected) R.string.network_type_connected else R.string.network_type_disconnected
}

@StringRes
fun NetworkInfo.typeLabelRes(): Int = type.labelRes(isConnected)

/** Pesan error yang ramah; bisa diformat dengan kode HTTP untuk [SpeedTestError.ServerError]. */
@Composable
fun SpeedTestError.message(): String = when (this) {
    SpeedTestError.NoConnection -> stringResource(R.string.error_no_connection)
    SpeedTestError.Timeout -> stringResource(R.string.error_timeout)
    is SpeedTestError.ServerError -> stringResource(R.string.error_server, httpCode)
    SpeedTestError.Unknown -> stringResource(R.string.error_unknown)
}

/** Teks grade seperti yang lazim ditampilkan alat bufferbloat (mis. "A+"). */
val BufferbloatGrade.label: String
    get() = when (this) {
        BufferbloatGrade.A_PLUS -> "A+"
        else -> name
    }
