package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.presentation.format.currentLocale
import io.github.willyrafaelfs.speedtest.presentation.format.formatDecimal
import io.github.willyrafaelfs.speedtest.presentation.format.fromMbps
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/**
 * Grafik throughput bergaya layar osiloskop: jejak download (cyan) dan upload (lime)
 * di sumbu yang sama. Sumbu X = progres di dalam fase, sehingga kedua jejak bisa
 * dibandingkan langsung. Dipakai saat test berjalan dan di layar detail hasil.
 *
 * @param highlightUpload true selama/setelah fase upload; jejak download lalu diredupkan.
 */
@Composable
fun ThroughputChart(
    download: List<TracePoint>,
    upload: List<TracePoint>,
    highlightUpload: Boolean,
    unit: SpeedUnit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val unitLabel = stringResource(unit.labelRes())
    val peakDown = unit.fromMbps((download.maxOfOrNull { it.mbps } ?: 0f).toDouble()).toFloat()
    val peakUp = unit.fromMbps((upload.maxOfOrNull { it.mbps } ?: 0f).toDouble()).toFloat()
    val axisMax = niceAxisMax(maxOf(peakDown, peakUp) * CHART_HEADROOM)
    val description = stringResource(
        R.string.cd_chart,
        formatDecimal(peakDown.toDouble(), 1, locale),
        formatDecimal(peakUp.toDouble(), 1, locale),
        unitLabel,
    )
    val colors = NetraTheme.colors
    val baseline = MaterialTheme.colorScheme.outline

    fun List<TracePoint>.normalized() = map {
        ChartPoint(
            it.progress,
            unit.fromMbps(it.mbps.toDouble()).toFloat() / axisMax,
        )
    }

    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChartLegendItem(stringResource(R.string.metric_download), colors.download)
            Spacer(Modifier.width(Spacing.lg))
            ChartLegendItem(stringResource(R.string.metric_upload), colors.upload)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.chart_axis_max, formatDecimal(axisMax.toDouble(), 0, locale), unitLabel),
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
            drawChartGrid(colors.grid, baseline)
            val downColor = if (highlightUpload) colors.download.copy(alpha = DIMMED_ALPHA) else colors.download
            drawLineSeries(download.normalized(), downColor)
            drawLineSeries(upload.normalized(), colors.upload)
        }
    }
}

@Preview
@Composable
private fun ThroughputChartPreview() {
    NetraTheme {
        ThroughputChart(
            download = List(40) { TracePoint(it / 39f, 80f + (it % 7) * 9f) },
            upload = List(20) { TracePoint(it / 39f, 30f + (it % 5) * 4f) },
            highlightUpload = true,
            unit = SpeedUnit.MBPS,
        )
    }
}
