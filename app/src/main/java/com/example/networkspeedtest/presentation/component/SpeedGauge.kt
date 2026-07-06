package com.example.networkspeedtest.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gauge speedometer custom berbasis Canvas.
 *
 * Peningkatan:
 * - Skala **non-linear** (0-25-50-100-250-500-1000 Mbps): resolusi lebih halus
 *   di kecepatan rendah, tapi jaringan cepat (fiber/5G) tetap terbaca wajar.
 * - Label angka skala digambar mengelilingi arc.
 * - Warna arc **dinamis** menurut kecepatan: merah (<10), oranye (10-50),
 *   hijau (>50) Mbps.
 * - Angka pusat, sudut arc, dan warna semuanya dianimasikan agar transisi mulus.
 */
@Composable
fun SpeedGauge(
    speedMbps: Float,
    modifier: Modifier = Modifier,
    unitLabel: String = "Mbps",
) {
    // Animasikan nilai kecepatan; arc & warna diturunkan darinya agar selalu sinkron.
    val animatedSpeed by animateFloatAsState(
        targetValue = speedMbps,
        animationSpec = tween(durationMillis = 500),
        label = "gaugeSpeed",
    )
    val fraction = speedToFraction(animatedSpeed)
    val animatedColor by animateColorAsState(
        targetValue = speedColor(animatedSpeed),
        animationSpec = tween(durationMillis = 500),
        label = "gaugeColor",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 10.sp)

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(260.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val arcRadius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Track penuh + arc progress (searah jarum jam, celah di bawah).
            drawArc(trackColor, START_ANGLE, SWEEP_ANGLE, false, topLeft, arcSize, style = stroke)
            drawArc(animatedColor, START_ANGLE, SWEEP_ANGLE * fraction, false, topLeft, arcSize, style = stroke)

            // Label skala di tiap breakpoint, ditata melingkar di dalam arc.
            val labelRadius = arcRadius - strokeWidth / 2f - 18.dp.toPx()
            val segments = SCALE_BREAKPOINTS.size - 1
            SCALE_BREAKPOINTS.forEachIndexed { index, value ->
                val angleDeg = START_ANGLE + SWEEP_ANGLE * (index.toFloat() / segments)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val x = center.x + labelRadius * cos(angleRad).toFloat()
                val y = center.y + labelRadius * sin(angleRad).toFloat()
                val layout = textMeasurer.measure(labelText(value), labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.1f", animatedSpeed),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Memetakan kecepatan ke fraksi 0f..1f di sepanjang arc menggunakan skala
 * non-linear: tiap breakpoint menempati porsi sudut yang sama, sehingga rentang
 * rendah (0-100) mendapat ruang lebih besar dan rentang tinggi terkompresi.
 */
private fun speedToFraction(speed: Float): Float {
    val bp = SCALE_BREAKPOINTS
    if (speed <= bp.first()) return 0f
    if (speed >= bp.last()) return 1f
    val segments = bp.size - 1
    for (i in 0 until segments) {
        if (speed < bp[i + 1]) {
            val within = (speed - bp[i]) / (bp[i + 1] - bp[i])
            return (i + within) / segments
        }
    }
    return 1f
}

private fun speedColor(speed: Float): Color = when {
    speed < 10f -> SlowColor
    speed < 50f -> MediumColor
    else -> FastColor
}

private fun labelText(value: Float): String =
    if (value >= 1000f) "1k" else value.toInt().toString()

// Breakpoint skala non-linear (Mbps).
private val SCALE_BREAKPOINTS = listOf(0f, 25f, 50f, 100f, 250f, 500f, 1000f)

// Warna semantik indikator kecepatan (bukan bagian tema; sengaja tetap merah/oranye/hijau).
private val SlowColor = Color(0xFFE53935)
private val MediumColor = Color(0xFFFB8C00)
private val FastColor = Color(0xFF2E7D32)

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
