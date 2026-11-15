package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.presentation.format.currentLocale
import io.github.willyrafaelfs.speedtest.presentation.format.formatDecimal
import io.github.willyrafaelfs.speedtest.presentation.format.formatShortDay
import io.github.willyrafaelfs.speedtest.presentation.format.fromMbps
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/**
 * Tren rata-rata harian. Untuk [TrendMetric.SPEED] menampilkan download dan upload,
 * untuk [TrendMetric.PING] menampilkan ping idle. Sumbu X = urutan hari yang punya data
 * (hari tanpa test tidak digambar), dengan tanggal pertama dan terakhir di bawah grafik.
 */
@Composable
fun TrendChart(
    stats: List<DailyStats>,
    metric: TrendMetric,
    unit: SpeedUnit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val colors = NetraTheme.colors
    val series = when (metric) {
        TrendMetric.SPEED -> listOf(
            Series(
                stringResource(R.string.metric_download),
                colors.download,
                stats.map {
                    unit.fromMbps(it.avgDownloadMbps)
                },
            ),
            Series(
                stringResource(R.string.metric_upload),
                colors.upload,
                stats.map {
                    unit.fromMbps(it.avgUploadMbps)
                },
            ),
        )

        TrendMetric.PING -> listOf(
            Series(
                stringResource(R.string.metric_ping),
                colors.latency,
                stats.map {
                    it.avgPingMs
                },
            ),
        )
    }
    val axisUnit = stringResource(if (metric == TrendMetric.PING) R.string.unit_ms else unit.labelRes())
    val axisMax = niceAxisMax((series.flatMap { it.values }.maxOrNull() ?: 0.0).toFloat() * CHART_HEADROOM)
    val description = pluralStringResource(R.plurals.cd_trend, stats.size, stats.size)
    val baseline = MaterialTheme.colorScheme.outline

    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            series.forEach { ChartLegendItem(it.label, it.color, Modifier.padding(end = Spacing.lg)) }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.chart_axis_max, formatDecimal(axisMax.toDouble(), 0, locale), axisUnit),
                style = NetraTheme.numerals.scale,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            drawChartGrid(colors.grid, baseline, columns = (stats.size - 1).coerceIn(1, MAX_GRID_COLUMNS))
            val lastIndex = (stats.size - 1).coerceAtLeast(1)
            series.forEach { s ->
                val points = s.values.mapIndexed { index, value ->
                    ChartPoint(
                        index.toFloat() / lastIndex,
                        value.toFloat() / axisMax,
                    )
                }
                drawLineSeries(points, s.color, markers = true)
            }
        }
        if (stats.isNotEmpty()) {
            Row(Modifier.padding(top = Spacing.xs)) {
                Text(
                    formatShortDay(stats.first().day, locale),
                    style = NetraTheme.numerals.scale,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatShortDay(stats.last().day, locale),
                    style = NetraTheme.numerals.scale,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class Series(val label: String, val color: Color, val values: List<Double>)

private const val MAX_GRID_COLUMNS = 10

@Preview
@Composable
private fun TrendChartPreview() {
    NetraTheme {
        TrendChart(
            stats = List(7) { DailyStats("2026-11-${it + 9}", 80.0 + it * 4, 20.0 + it, 24.0 - it, 2) },
            metric = TrendMetric.SPEED,
            unit = SpeedUnit.MBPS,
        )
    }
}
