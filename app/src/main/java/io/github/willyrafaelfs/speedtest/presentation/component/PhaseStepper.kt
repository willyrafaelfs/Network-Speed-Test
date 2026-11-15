package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/**
 * Tiga tahap test (ping → download → upload) sebagai segmen berurutan. Tahap yang
 * sudah lewat terisi penuh, tahap aktif mengikuti [progressFraction], sisanya kosong.
 */
@Composable
fun PhaseStepper(
    phase: SpeedTestPhase,
    progressFraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = NetraTheme.colors
    val steps = listOf(
        Step(SpeedTestPhase.PING, stringResource(R.string.metric_ping), colors.latency),
        Step(SpeedTestPhase.DOWNLOAD, stringResource(R.string.metric_download), colors.download),
        Step(SpeedTestPhase.UPLOAD, stringResource(R.string.metric_upload), colors.upload),
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        steps.forEach { step ->
            StepSegment(
                step = step,
                fill = step.fillFor(phase, progressFraction),
                isActive = step.phase == phase,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private data class Step(val phase: SpeedTestPhase, val label: String, val color: Color) {
    fun fillFor(current: SpeedTestPhase, progress: Float): Float = when {
        current == SpeedTestPhase.FINISHED -> 1f
        current == SpeedTestPhase.IDLE -> 0f
        current.ordinal > phase.ordinal -> 1f
        current == phase -> progress
        else -> 0f
    }
}

@Composable
private fun StepSegment(step: Step, fill: Float, isActive: Boolean, modifier: Modifier = Modifier) {
    val animatedFill by animateFloatAsState(fill, label = "stepFill")
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            progressBarRangeInfo = ProgressBarRangeInfo(fill, 0f..1f)
        },
    ) {
        Text(
            text = step.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier
                .padding(top = Spacing.xs)
                .fillMaxWidth()
                .height(3.dp)
                .background(NetraTheme.colors.grid, MaterialTheme.shapes.extraSmall),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFill)
                    .background(step.color, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}
