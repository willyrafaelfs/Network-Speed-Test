package com.example.networkspeedtest.presentation.component

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Gauge speedometer custom berbasis Canvas: busur 270° yang terisi sesuai
 * [speedMbps] relatif terhadap [maxMbps], dengan angka besar animasi di tengah.
 *
 * Baik sudut busur maupun angka dianimasikan agar terlihat "naik" mulus saat
 * sampel kecepatan real-time berdatangan.
 */
@Composable
fun SpeedGauge(
    speedMbps: Float,
    modifier: Modifier = Modifier,
    maxMbps: Float = 100f,
    unitLabel: String = "Mbps",
) {
    val fraction = (speedMbps / maxMbps).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 400),
        label = "gaugeFraction",
    )
    val animatedSpeed by animateFloatAsState(
        targetValue = speedMbps,
        animationSpec = tween(durationMillis = 400),
        label = "gaugeSpeed",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(240.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 22.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Busur mulai 135° memutar 270° searah jarum jam (celah di bawah).
            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = progressColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
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

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
