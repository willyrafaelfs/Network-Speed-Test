package io.github.willyrafaelfs.speedtest.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.BufferbloatGrade
import io.github.willyrafaelfs.speedtest.presentation.format.label
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

/** Wadah panel instrumen: permukaan sedikit terangkat dengan garis tepi tipis; bisa diketuk bila [onClick] diisi. */
@Composable
fun InstrumentPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val color = MaterialTheme.colorScheme.surfaceContainerLow
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val inner: @Composable () -> Unit = { Column(modifier = Modifier.padding(Spacing.lg), content = content) }
    if (onClick == null) {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border, content = inner)
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = border,
            content = inner,
        )
    }
}

/**
 * Nilai utama (download/upload): penanda warna kanal, label, lalu angka besar
 * dengan satuannya. Dibaca screen reader sebagai satu kalimat.
 */
@Composable
fun PrimaryReadout(
    label: String,
    value: String,
    unit: String,
    channelColor: Color,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = NetraTheme.numerals.readout,
) {
    // Nilai belum terukur ("—") ditampilkan tanpa satuan.
    val shownUnit = if (value == stringResource(R.string.value_missing)) "" else unit
    val description = stringResource(R.string.cd_metric_value, label, value, shownUnit)
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelMark(channelColor)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = Spacing.xs)) {
            Text(
                text = value,
                style = valueStyle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = shownUnit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .alignByBaseline()
                    .padding(start = Spacing.xs),
            )
        }
    }
}

/** Baris readout sekunder: label rata kiri, nilai tabular rata kanan. */
@Composable
fun ReadoutRow(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    channelColor: Color? = null,
) {
    val shownUnit = if (value == stringResource(R.string.value_missing)) "" else unit
    val description = stringResource(R.string.cd_metric_value, label, value, shownUnit)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (channelColor != null) {
            ChannelMark(channelColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (channelColor != null) Spacing.sm else 0.dp),
        )
        Text(
            text = value,
            style = NetraTheme.numerals.readoutSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = shownUnit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .alignByBaseline()
                .widthIn(min = 28.dp)
                .padding(start = Spacing.xs),
        )
    }
}

/** Lencana grade bufferbloat; tanda kosong bila belum ada pengukuran saat beban. */
@Composable
fun GradeBadge(grade: BufferbloatGrade?, modifier: Modifier = Modifier) {
    val text = grade?.label ?: stringResource(R.string.value_missing)
    val description = grade?.let { stringResource(R.string.cd_bufferbloat_grade, it.label) }.orEmpty()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .widthIn(min = 40.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = text,
            style = NetraTheme.numerals.readoutSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/** Garis vertikal pendek berwarna kanal, penanda metrik mana yang sedang dibaca. */
@Composable
private fun ChannelMark(color: Color) {
    Box(
        Modifier
            .width(3.dp)
            .height(14.dp)
            .background(color, MaterialTheme.shapes.extraSmall),
    )
}

/** Garis pemisah tipis antar kelompok readout. */
@Composable
fun ReadoutDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
