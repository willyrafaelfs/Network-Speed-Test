package io.github.willyrafaelfs.speedtest.ui.theme

import androidx.compose.ui.graphics.Color

// Palet Netra: estetika alat ukur. Dasar navy pekat, dan tiga warna kanal yang
// selalu bermakna sama di seluruh aplikasi: cyan = download, lime = upload, amber = latency.
// Varian terang memakai tone yang lebih gelap agar kontras teks tetap >= 4.5:1.

// --- Gelap (default) ---
internal val InkDark = Color(0xFF0B1220)
internal val SurfaceDark = Color(0xFF0E1628)
internal val SurfaceContainerLowDark = Color(0xFF111A2E)
internal val SurfaceContainerDark = Color(0xFF142036)
internal val SurfaceContainerHighDark = Color(0xFF1A2741)
internal val OnInkDark = Color(0xFFE6EDF7)
internal val MutedDark = Color(0xFF8D9AB3)
internal val OutlineDark = Color(0xFF2E3D5E)
internal val OutlineVariantDark = Color(0xFF1F2B45)

internal val CyanDark = Color(0xFF22D3EE)
internal val OnCyanDark = Color(0xFF032530)
internal val CyanContainerDark = Color(0xFF0C3A48)
internal val OnCyanContainerDark = Color(0xFFA5F3FC)
internal val LimeDark = Color(0xFFA3E635)
internal val AmberDark = Color(0xFFF59E0B)
internal val OnAmberDark = Color(0xFF2B1700)

// Peran "secondary" Material (indikator navigasi, chip, dsb.) memakai slate netral,
// BUKAN warna kanal, agar lime tetap hanya berarti "upload".
internal val SlateDark = Color(0xFFA9B8D3)
internal val OnSlateDark = Color(0xFF0B1220)
internal val SlateContainerDark = Color(0xFF223050)
internal val OnSlateContainerDark = Color(0xFFDCE6F5)
internal val AmberContainerDark = Color(0xFF4A2F06)
internal val OnAmberContainerDark = Color(0xFFFDE7C2)
internal val ErrorContainerDark = Color(0xFF5C1717)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)
internal val ErrorDark = Color(0xFFF87171)
internal val OnErrorDark = Color(0xFF3B0A0A)

// --- Terang ---
internal val InkLight = Color(0xFFEEF2F8)
internal val SurfaceLight = Color(0xFFF5F7FB)
internal val SurfaceContainerLowLight = Color(0xFFFFFFFF)
internal val SurfaceContainerLight = Color(0xFFFFFFFF)
internal val SurfaceContainerHighLight = Color(0xFFE4EAF4)
internal val OnInkLight = Color(0xFF0B1220)
internal val MutedLight = Color(0xFF4A5873)
internal val OutlineLight = Color(0xFFAEBBD0)
internal val OutlineVariantLight = Color(0xFFD5DDEA)

internal val CyanLight = Color(0xFF0E7490)
internal val CyanContainerLight = Color(0xFFCFF5FB)
internal val OnCyanContainerLight = Color(0xFF083344)
internal val LimeLight = Color(0xFF4D7C0F)
internal val AmberLight = Color(0xFFB45309)
internal val ErrorLight = Color(0xFFB91C1C)
internal val SlateLight = Color(0xFF4A5873)
internal val SlateContainerLight = Color(0xFFDCE4F0)
internal val OnSlateContainerLight = Color(0xFF0B1220)
internal val AmberContainerLight = Color(0xFFFDE7C2)
internal val OnAmberContainerLight = Color(0xFF4A2F06)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF5C1717)
