package io.github.willyrafaelfs.speedtest.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Warna semantik Netra di luar color scheme Material. Warna kanal tetap dipakai
 * walau dynamic color aktif, karena maknanya (download/upload/latency) harus konsisten.
 */
@Immutable
data class NetraColors(
    val download: Color,
    val upload: Color,
    val latency: Color,
    /** Garis grid grafik dan tanda skala minor dial. */
    val grid: Color,
)

private val DarkNetraColors = NetraColors(
    download = CyanDark,
    upload = LimeDark,
    latency = AmberDark,
    grid = OutlineVariantDark,
)

private val LightNetraColors = NetraColors(
    download = CyanLight,
    upload = LimeLight,
    latency = AmberLight,
    grid = OutlineVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = CyanDark,
    onPrimary = OnCyanDark,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = OnCyanContainerDark,
    secondary = SlateDark,
    onSecondary = OnSlateDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = OnSlateContainerDark,
    tertiary = AmberDark,
    onTertiary = OnAmberDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surfaceTint = CyanDark,
    inverseSurface = OnInkDark,
    inverseOnSurface = InkDark,
    inversePrimary = CyanLight,
    background = InkDark,
    onBackground = OnInkDark,
    surface = SurfaceDark,
    onSurface = OnInkDark,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = MutedDark,
    surfaceContainerLowest = InkDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = CyanLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = OnCyanContainerLight,
    secondary = SlateLight,
    onSecondary = Color.White,
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = OnSlateContainerLight,
    tertiary = AmberLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    surfaceTint = CyanLight,
    inverseSurface = InkDark,
    inverseOnSurface = OnInkDark,
    inversePrimary = CyanDark,
    background = InkLight,
    onBackground = OnInkLight,
    surface = SurfaceLight,
    onSurface = OnInkLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = MutedLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

/** Radius kecil khas instrumen; makin besar elemennya, makin besar radiusnya. */
private val NetraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/** Token jarak (kelipatan 4 dp). */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

private val LocalNetraColors = staticCompositionLocalOf { DarkNetraColors }
private val LocalNetraNumerals = staticCompositionLocalOf { NetraNumerals() }

/** Akses token Netra di luar MaterialTheme. */
object NetraTheme {
    val colors: NetraColors
        @Composable @ReadOnlyComposable
        get() = LocalNetraColors.current

    val numerals: NetraNumerals
        @Composable @ReadOnlyComposable
        get() = LocalNetraNumerals.current
}

/**
 * Tema aplikasi. Palet Netra dipakai secara default; [dynamicColor] (Android 12+)
 * hanya mengganti warna Material, sementara warna kanal tetap.
 */
@Composable
fun NetraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalNetraColors provides if (darkTheme) DarkNetraColors else LightNetraColors,
        LocalNetraNumerals provides NetraNumerals(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NetraTypography,
            shapes = NetraShapes,
            content = content,
        )
    }
}
