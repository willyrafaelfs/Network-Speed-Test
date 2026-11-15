package io.github.willyrafaelfs.speedtest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.willyrafaelfs.speedtest.R

/** IBM Plex Sans untuk seluruh teks UI. */
val PlexSans = FontFamily(
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
)

/** IBM Plex Mono khusus angka hasil ukur: lebar digit seragam sehingga angka tidak "goyang" saat berubah. */
val PlexMono = FontFamily(
    Font(R.font.plex_mono_regular, FontWeight.Normal),
    Font(R.font.plex_mono_medium, FontWeight.Medium),
    Font(R.font.plex_mono_semibold, FontWeight.SemiBold),
)

private val base = Typography()

/** Skala tipe Material 3 dengan Plex Sans; bobot dinaikkan sedikit untuk judul dan label. */
val NetraTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = PlexSans),
    displayMedium = base.displayMedium.copy(fontFamily = PlexSans),
    displaySmall = base.displaySmall.copy(fontFamily = PlexSans),
    headlineLarge = base.headlineLarge.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = PlexSans),
    bodyMedium = base.bodyMedium.copy(fontFamily = PlexSans),
    bodySmall = base.bodySmall.copy(fontFamily = PlexSans),
    labelLarge = base.labelLarge.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = PlexSans, fontWeight = FontWeight.Medium),
)

/** Gaya angka hasil ukur (Plex Mono), di luar skala Material. */
data class NetraNumerals(
    /** Angka besar di tengah dial. */
    val dial: TextStyle = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
    ),
    /** Nilai utama di panel readout (download/upload). */
    val readout: TextStyle = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    /** Nilai sekunder (ping, jitter, latency saat beban, riwayat). */
    val readoutSmall: TextStyle = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    /** Label skala dial & sumbu grafik. */
    val scale: TextStyle = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    ),
)
