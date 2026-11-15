package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.presentation.format.currentLocale
import io.github.willyrafaelfs.speedtest.presentation.format.formatDecimal
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dial instrumen: cincin tanda skala (minor & mayor) dengan label, arc nilai berwarna
 * kanal, dan angka besar di tengah.
 *
 * Skala non-linear (0-25-50-100-250-500-1000 Mbps): tiap breakpoint menempati sudut
 * yang sama, sehingga kecepatan rendah tetap terbaca detail dan jaringan gigabit tetap muat.
 *
 * @param speedMbps nilai yang ditunjukkan; perubahan dianimasikan.
 * @param channelColor warna kanal fase aktif (download/upload/latency).
 * @param caption teks kecil di bawah satuan (mis. fase yang sedang berjalan).
 */
@Composable
fun InstrumentDial(
    speedMbps: Float,
    channelColor: Color,
    caption: String,
    modifier: Modifier = Modifier,
    maxSize: Dp = 300.dp,
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedMbps,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "dialSpeed",
    )
    val animatedColor by animateColorAsState(channelColor, tween(DIAL_COLOR_MILLIS), label = "dialColor")

    val locale = currentLocale()
    val description = stringResource(R.string.cd_dial, formatDecimal(speedMbps.toDouble(), 1, locale))
    val colors = DialColors(
        value = animatedColor,
        track = NetraTheme.colors.grid,
        minorTick = NetraTheme.colors.grid,
        majorTick = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val scaleStyle = NetraTheme.numerals.scale.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()

    // Arc 270 derajat tidak memakai seperempat bawah lingkaran, jadi tinggi dipangkas
    // dan pusat dial berada di (lebar/2, lebar/2), bukan di tengah kotak.
    Box(
        modifier = modifier
            .widthIn(max = maxSize)
            .aspectRatio(DIAL_ASPECT_RATIO)
            .semantics { contentDescription = description },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDial(speedToFraction(animatedSpeed), colors, textMeasurer, scaleStyle)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.centerOnDial(),
        ) {
            Text(
                text = formatDecimal(animatedSpeed.toDouble(), 1, locale),
                style = NetraTheme.numerals.dial,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.unit_mbps),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedContent(
                targetState = caption,
                transitionSpec = {
                    fadeIn(tween(CAPTION_FADE_MILLIS)) togetherWith fadeOut(tween(CAPTION_FADE_MILLIS))
                },
                label = "dialCaption",
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Menempatkan konten di tengah horizontal dengan pusat vertikal di titik pusat dial. */
private fun Modifier.centerOnDial() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(
            x = (constraints.maxWidth - placeable.width) / 2,
            y = constraints.maxWidth / 2 - placeable.height / 2,
        )
    }
}

private data class DialColors(val value: Color, val track: Color, val minorTick: Color, val majorTick: Color)

private fun DrawScope.drawDial(fraction: Float, colors: DialColors, textMeasurer: TextMeasurer, scaleStyle: TextStyle) {
    val center = Offset(size.width / 2f, size.width / 2f)
    val outer = size.width / 2f - 2.dp.toPx()
    val arcRadius = outer - 24.dp.toPx()
    val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
    val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
    val arcStroke = 6.dp.toPx()

    // Tanda skala: minor di antara breakpoint, mayor (lebih panjang) di breakpoint.
    val segments = SCALE_BREAKPOINTS.size - 1
    val totalTicks = segments * MINOR_PER_SEGMENT
    for (tick in 0..totalTicks) {
        val isMajor = tick % MINOR_PER_SEGMENT == 0
        val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * tick / totalTicks).toDouble())
        val length = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
        drawLine(
            color = if (isMajor) colors.majorTick else colors.minorTick,
            start = center + polar(outer - length, angle),
            end = center + polar(outer, angle),
            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Label skala di dalam arc nilai.
    val labelRadius = arcRadius - 22.dp.toPx()
    SCALE_BREAKPOINTS.forEachIndexed { index, value ->
        val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * index / segments).toDouble())
        val layout = textMeasurer.measure(scaleLabel(value), scaleStyle)
        val position = center + polar(labelRadius, angle)
        drawText(layout, topLeft = Offset(position.x - layout.size.width / 2f, position.y - layout.size.height / 2f))
    }

    // Track, cahaya lembut, lalu arc nilai dan penunjuk di ujungnya.
    drawArc(colors.track, START_ANGLE, SWEEP_ANGLE, false, arcTopLeft, arcSize, style = Stroke(arcStroke))
    if (fraction > 0f) {
        val sweep = SWEEP_ANGLE * fraction
        drawArc(
            color = colors.value.copy(alpha = GLOW_ALPHA),
            startAngle = START_ANGLE,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(arcStroke * GLOW_WIDTH_FACTOR, cap = StrokeCap.Round),
        )
        drawArc(
            color = colors.value,
            startAngle = START_ANGLE,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(arcStroke, cap = StrokeCap.Round),
        )
        val tip = center + polar(arcRadius, Math.toRadians((START_ANGLE + sweep).toDouble()))
        drawCircle(colors.value, radius = 5.dp.toPx(), center = tip)
    }
}

private fun polar(radius: Float, angleRad: Double) =
    Offset((radius * cos(angleRad)).toFloat(), (radius * sin(angleRad)).toFloat())

/** Fraksi 0f..1f di sepanjang arc untuk skala non-linear [SCALE_BREAKPOINTS]. */
internal fun speedToFraction(speed: Float): Float {
    val bp = SCALE_BREAKPOINTS
    val segments = bp.size - 1
    val clamped = speed.coerceIn(bp.first(), bp.last())
    val segment = (0 until segments).firstOrNull { clamped < bp[it + 1] } ?: return 1f
    val within = (clamped - bp[segment]) / (bp[segment + 1] - bp[segment])
    return (segment + within) / segments
}

private fun scaleLabel(value: Float): String = if (value >= GIGABIT_MBPS) "1k" else value.toInt().toString()

private val SCALE_BREAKPOINTS = listOf(0f, 25f, 50f, 100f, 250f, 500f, 1000f)
private const val MINOR_PER_SEGMENT = 5

// lebar : tinggi; tinggi = r + r*sin(45°) + ruang tanda skala.
private const val DIAL_ASPECT_RATIO = 1.15f
private const val GIGABIT_MBPS = 1000f
private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
private const val GLOW_ALPHA = 0.16f
private const val GLOW_WIDTH_FACTOR = 3f
private const val DIAL_COLOR_MILLIS = 400
private const val CAPTION_FADE_MILLIS = 220

@Preview
@Composable
private fun InstrumentDialPreview() {
    NetraTheme {
        InstrumentDial(speedMbps = 187.4f, channelColor = NetraTheme.colors.download, caption = "Measuring download")
    }
}
