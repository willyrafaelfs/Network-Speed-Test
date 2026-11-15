package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.presentation.format.currentLocale
import io.github.willyrafaelfs.speedtest.presentation.format.formatDecimal
import io.github.willyrafaelfs.speedtest.presentation.speedtest.TracePoint
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/**
 * Grafik throughput real-time bergaya layar osiloskop: grid halus, jejak download
 * (cyan) dan upload (lime) di sumbu yang sama. Sumbu X = progres di dalam fase,
 * sehingga kedua jejak bisa dibandingkan langsung.
 *
 * @param highlightUpload true selama/ setelah fase upload; jejak download lalu diredupkan.
 */
@Composable
fun LiveTraceChart(
    download: List<TracePoint>,
    upload: List<TracePoint>,
    highlightUpload: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val peakDown = download.maxOfOrNull { it.mbps } ?: 0f
    val peakUp = upload.maxOfOrNull { it.mbps } ?: 0f
    val axisMax = niceAxisMax(maxOf(peakDown, peakUp) * HEADROOM)
    val description = stringResource(
        R.string.cd_chart,
        formatDecimal(peakDown.toDouble(), 1, locale),
        formatDecimal(peakUp.toDouble(), 1, locale),
    )
    val colors = NetraTheme.colors
    val baseline = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendItem(stringResource(R.string.metric_download), colors.download)
            Spacer(Modifier.width(Spacing.lg))
            LegendItem(stringResource(R.string.metric_upload), colors.upload)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.chart_axis_max, formatDecimal(axisMax.toDouble(), 0, locale)),
                style = NetraTheme.numerals.scale,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawGrid(colors.grid, baseline)
                val downAlpha = if (highlightUpload) DIMMED_ALPHA else 1f
                drawTrace(download, axisMax, colors.download.copy(alpha = downAlpha))
                drawTrace(upload, axisMax, colors.upload)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
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

private fun DrawScope.drawGrid(grid: Color, baseline: Color) {
    val hairline = 1.dp.toPx()
    for (row in 0 until GRID_ROWS) {
        val y = size.height * row / GRID_ROWS
        drawLine(grid, Offset(0f, y), Offset(size.width, y), hairline)
    }
    for (col in 1 until GRID_COLUMNS) {
        val x = size.width * col / GRID_COLUMNS
        drawLine(grid, Offset(x, 0f), Offset(x, size.height), hairline)
    }
    drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), hairline)
}

private fun DrawScope.drawTrace(points: List<TracePoint>, axisMax: Float, color: Color) {
    if (points.size < 2) return
    val line = Path()
    points.forEachIndexed { index, point ->
        val x = point.progress.coerceIn(0f, 1f) * size.width
        val y = size.height - (point.mbps / axisMax).coerceIn(0f, 1f) * size.height
        if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
    }
    val fill = Path().apply {
        addPath(line)
        lineTo(points.last().progress.coerceIn(0f, 1f) * size.width, size.height)
        lineTo(points.first().progress.coerceIn(0f, 1f) * size.width, size.height)
        close()
    }
    drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = color.alpha * FILL_ALPHA), Color.Transparent)))
    drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Batas atas sumbu Y yang "bulat" (10, 25, 50, 100, 250, ...) dan minimal 10 Mbps. */
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

private val NICE_STEPS = listOf(1f, 2.5f, 5f)
private const val MIN_AXIS_MAX = 10f
private const val DECADE = 10f
private const val HEADROOM = 1.15f
private const val GRID_ROWS = 4
private const val GRID_COLUMNS = 10
private const val DIMMED_ALPHA = 0.45f
private const val FILL_ALPHA = 0.22f

@Preview
@Composable
private fun LiveTraceChartPreview() {
    NetraTheme {
        LiveTraceChart(
            download = List(40) { TracePoint(it / 39f, 80f + (it % 7) * 9f) },
            upload = List(20) { TracePoint(it / 39f, 30f + (it % 5) * 4f) },
            highlightUpload = true,
        )
    }
}
