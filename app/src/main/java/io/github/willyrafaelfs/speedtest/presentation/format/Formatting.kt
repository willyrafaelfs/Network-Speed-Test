package io.github.willyrafaelfs.speedtest.presentation.format

import android.text.format.DateFormat
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
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
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

/** Nilai kecepatan dalam satuan tampilan (data selalu disimpan dalam Mbps). */
fun SpeedUnit.fromMbps(mbps: Double): Double = when (this) {
    SpeedUnit.MBPS -> mbps
    SpeedUnit.MEGABYTES_PER_SECOND -> mbps / BITS_PER_BYTE
}

/** Kecepatan dalam [unit] dengan 1 angka desimal, atau tanda kosong bila belum terukur. */
@Composable
@ReadOnlyComposable
fun formatSpeed(mbps: Double?, unit: SpeedUnit): String =
    mbps?.let { formatDecimal(unit.fromMbps(it), 1, currentLocale()) } ?: stringResource(R.string.value_missing)

private const val BITS_PER_BYTE = 8.0

/** Tanggal & jam sesuai locale UI; formatter dibuat per panggilan karena DateFormat tidak thread-safe. */
@Composable
@ReadOnlyComposable
fun formatTimestamp(timestamp: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, currentLocale())
        .format(Date(timestamp))

/** Tanggal ISO "yyyy-MM-dd" sebagai label pendek sesuai locale (mis. "Nov 15" / "15 Nov"). */
fun formatShortDay(isoDay: String, locale: Locale): String {
    val date = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDay) }.getOrNull() ?: return isoDay
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMd")
    return SimpleDateFormat(pattern, locale).format(date)
}

/** Latency dalam ms tanpa desimal, atau tanda kosong bila belum terukur. */
@Composable
@ReadOnlyComposable
fun formatMs(value: Double?): String =
    value?.let { formatDecimal(it, 0, currentLocale()) } ?: stringResource(R.string.value_missing)
