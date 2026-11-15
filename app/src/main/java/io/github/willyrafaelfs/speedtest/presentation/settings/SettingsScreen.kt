package io.github.willyrafaelfs.speedtest.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.willyrafaelfs.speedtest.BuildConfig
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.domain.model.AppSettings
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.ThemeMode
import io.github.willyrafaelfs.speedtest.presentation.component.InstrumentPanel
import io.github.willyrafaelfs.speedtest.presentation.component.ReadoutDivider
import io.github.willyrafaelfs.speedtest.presentation.format.labelRes
import io.github.willyrafaelfs.speedtest.ui.theme.NetraTheme
import io.github.willyrafaelfs.speedtest.ui.theme.Spacing

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsContent(
        settings = settings ?: AppSettings(),
        actions = SettingsActions(
            onDurationChange = viewModel::setTestDuration,
            onStreamCountChange = viewModel::setStreamCount,
            onSpeedUnitChange = viewModel::setSpeedUnit,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor,
        ),
        modifier = modifier,
    )
}

/** Callback layar Settings, dikelompokkan agar content tetap ringkas. */
data class SettingsActions(
    val onDurationChange: (Int) -> Unit = {},
    val onStreamCountChange: (Int) -> Unit = {},
    val onSpeedUnitChange: (SpeedUnit) -> Unit = {},
    val onThemeModeChange: (ThemeMode) -> Unit = {},
    val onDynamicColorChange: (Boolean) -> Unit = {},
)

@Composable
private fun SettingsContent(settings: AppSettings, actions: SettingsActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        SettingsSection(stringResource(R.string.settings_section_measurement)) {
            ChoiceSetting(
                title = stringResource(R.string.settings_duration),
                hint = stringResource(R.string.settings_duration_hint),
                options = AppSettings.DURATION_OPTIONS,
                selected = settings.testDurationSeconds,
                label = { stringResource(R.string.settings_duration_value, it) },
                onSelect = actions.onDurationChange,
            )
            ReadoutDivider()
            ChoiceSetting(
                title = stringResource(R.string.settings_streams),
                hint = stringResource(R.string.settings_streams_hint),
                options = AppSettings.STREAM_OPTIONS,
                selected = settings.streamCount,
                label = { it.toString() },
                onSelect = actions.onStreamCountChange,
            )
        }

        SettingsSection(stringResource(R.string.settings_section_display)) {
            ChoiceSetting(
                title = stringResource(R.string.settings_unit),
                options = SpeedUnit.entries,
                selected = settings.speedUnit,
                label = { stringResource(it.labelRes()) },
                onSelect = actions.onSpeedUnitChange,
            )
            ReadoutDivider()
            ChoiceSetting(
                title = stringResource(R.string.settings_theme),
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = { stringResource(it.labelRes()) },
                onSelect = actions.onThemeModeChange,
            )
            // Dynamic color hanya ada di Android 12+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ReadoutDivider()
                SwitchSetting(
                    title = stringResource(R.string.settings_dynamic_color),
                    hint = stringResource(R.string.settings_dynamic_color_hint),
                    checked = settings.dynamicColor,
                    onCheckedChange = actions.onDynamicColorChange,
                )
            }
        }

        SettingsSection(stringResource(R.string.settings_section_about)) {
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.settings_methodology),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        InstrumentPanel { content() }
    }
}

@Composable
private fun <T> ChoiceSetting(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = {},
                ) {
                    Text(label(option), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Seluruh baris bisa diketuk dan dibaca sebagai satu switch oleh screen reader.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1220, heightDp = 900)
@Composable
private fun SettingsContentPreview() {
    NetraTheme(darkTheme = true) {
        SettingsContent(settings = AppSettings(), actions = SettingsActions())
    }
}
