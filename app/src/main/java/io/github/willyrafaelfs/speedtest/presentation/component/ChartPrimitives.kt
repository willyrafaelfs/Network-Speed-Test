package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/** Grid halus bergaya layar osiloskop, dengan garis dasar yang lebih tegas. */
internal fun DrawScope.drawChartGrid(grid: Color, baseline: Color, rows: Int = GRID_ROWS, columns: Int = GRID_COLUMNS) {
    val hairline = 1.dp.toPx()
    for (row in 0 until rows) {
        val y = size.height * row / rows
        drawLine(grid, Offset(0f, y), Offset(size.width, y), hairline)
    }
    for (col in 1 until columns) {
        val x = size.width * col / columns
        drawLine(grid, Offset(x, 0f), Offset(x, size.height), hairline)
    }
    drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), hairline)
}

/**
 * Satu seri garis dengan isian gradasi di bawahnya. [markers] menggambar titik di setiap
 * data, berguna saat jumlah titik sedikit (mis. tren harian).
 */
internal fun DrawScope.drawLineSeries(points: List<ChartPoint>, color: Color, markers: Boolean = false) {
    if (points.isEmpty()) return
    val offsets = points.map {
        Offset(
            it.x.coerceIn(0f, 1f) * size.width,
            size.height - it.y.coerceIn(0f, 1f) * size.height,
        )
    }
    if (offsets.size >= 2) {
        val line = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(offsets.last().x, size.height)
            lineTo(offsets.first().x, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = color.alpha * FILL_ALPHA), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    if (markers || offsets.size == 1) {
        offsets.forEach { drawCircle(color, radius = 3.dp.toPx(), center = it) }
    }
}

/** Keterangan warna seri. */
@Composable
internal fun ChartLegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Batas atas sumbu Y yang "bulat" (10, 25, 50, 100, 250, ...), minimal 10. */
internal fun niceAxisMax(value: Float): Float {
    var magnitude = MIN_AXIS_MAX
    while (true) {
        for (step in NICE_STEPS) {
            val candidate = step * magnitude
            if (value <= candidate) return candidate
        }
        magnitude *= DECADE
    }
}

/** Ruang kosong di atas nilai tertinggi agar garis tidak menempel di tepi atas. */
internal const val CHART_HEADROOM = 1.15f

/** Alpha untuk seri yang sedang tidak disorot. */
internal const val DIMMED_ALPHA = 0.45f

private val NICE_STEPS = listOf(1f, 2.5f, 5f)
private const val MIN_AXIS_MAX = 10f
private const val DECADE = 10f
private const val GRID_ROWS = 4
private const val GRID_COLUMNS = 10
private const val FILL_ALPHA = 0.22f
