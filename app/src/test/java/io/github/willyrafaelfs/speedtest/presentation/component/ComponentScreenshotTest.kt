package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.willyrafaelfs.speedtest.domain.model.DailyStats
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot test komponen utama (terang & gelap). Baseline ada di app/src/test/screenshots;
 * `./gradlew recordRoborazziDebug` memperbarui baseline, `verifyRoborazziDebug` membandingkan.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
class ComponentScreenshotTest {

    @Test
    fun dial_light() = capture("dial_light", dark = false) {
        InstrumentDial(
            speedMbps = 187.4f,
            channelColor = NetraTheme.colors.download,
            caption = "Measuring download",
            unit = SpeedUnit.MBPS,
        )
    }

    @Test
    fun dial_dark_megabytes() = capture("dial_dark_megabytes", dark = true) {
        InstrumentDial(
            speedMbps = 48f,
            channelColor = NetraTheme.colors.upload,
            caption = "Measuring upload",
            unit = SpeedUnit.MEGABYTES_PER_SECOND,
        )
    }

    @Test
    fun throughput_chart_light() = capture("throughput_chart_light", dark = false) {
        ThroughputChart(download = DOWNLOAD, upload = UPLOAD, highlightUpload = true, unit = SpeedUnit.MBPS)
    }

    @Test
    fun throughput_chart_dark() = capture("throughput_chart_dark", dark = true) {
        ThroughputChart(download = DOWNLOAD, upload = UPLOAD, highlightUpload = false, unit = SpeedUnit.MBPS)
    }

    @Test
    fun result_readouts_dark() = capture("result_readouts_dark", dark = true) {
        ResultReadoutsPanel(metrics = METRICS, unit = SpeedUnit.MBPS)
    }

    @Test
    fun result_readouts_light() = capture("result_readouts_light", dark = false) {
        ResultReadoutsPanel(metrics = METRICS, unit = SpeedUnit.MBPS)
    }

    @Test
    fun trend_chart_light() = capture("trend_chart_light", dark = false) {
        TrendChart(stats = STATS, metric = TrendMetric.SPEED, unit = SpeedUnit.MBPS)
    }

    @Test
    fun trend_chart_ping_dark() = capture("trend_chart_ping_dark", dark = true) {
        TrendChart(stats = STATS, metric = TrendMetric.PING, unit = SpeedUnit.MBPS)
    }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) =
        captureRoboImage("src/test/screenshots/$name.png") {
            NetraTheme(darkTheme = dark) {
                Box(
                    Modifier
                        .width(360.dp)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                ) { content() }
            }
        }

    private companion object {
        val DOWNLOAD = List(40) { TracePoint(it / 39f, 120f + (it % 7) * 9f - if (it < 6) (6 - it) * 18f else 0f) }
        val UPLOAD = List(40) { TracePoint(it / 39f, 28f + (it % 5) * 3f) }
        val METRICS = ResultMetrics(187.4, 41.2, 18.0, 2.0, 61.0, 48.0)
        val STATS = List(7) {
            DailyStats(
                "2026-11-${it + 9}",
                80.0 + it * 4 + (it % 2) * 9,
                20.0 + it,
                24.0 - it + (it % 3) * 3,
                2,
            )
        }
    }
}
