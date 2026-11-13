/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.feature.settings.freshness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.freshness.FreshnessSettings
import org.meshtastic.core.model.freshness.NetworkFreshnessState
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.command_post_duration_hours
import org.meshtastic.core.resources.command_post_duration_minutes
import org.meshtastic.core.resources.command_post_threshold_auto
import org.meshtastic.core.resources.command_post_threshold_fixed
import org.meshtastic.core.resources.freshness_setting_auto
import org.meshtastic.core.resources.freshness_setting_auto_summary
import org.meshtastic.core.resources.freshness_setting_fixed
import org.meshtastic.core.resources.freshness_setting_fixed_summary
import org.meshtastic.core.resources.freshness_setting_interval
import org.meshtastic.core.resources.freshness_setting_interval_summary
import org.meshtastic.core.resources.freshness_setting_pc_only
import org.meshtastic.core.resources.freshness_setting_summary
import org.meshtastic.core.resources.freshness_setting_title
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.SwitchPreference
import kotlin.time.Duration

private const val MINUTES_PER_HOUR = 60

/** The settings card, wired to its view model. */
@Composable
fun FreshnessSettingHost(modifier: Modifier = Modifier, viewModel: FreshnessSettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val commandPostMode by viewModel.commandPostMode.collectAsStateWithLifecycle()
    FreshnessSettingCard(
        state = state,
        editable = commandPostMode,
        onChange = { viewModel.update(it) },
        modifier = modifier,
    )
}

/**
 * "Freshness of positions": the threshold in force in plain words, then the expected interval, the scaling switch and
 * the fixed threshold. Everything is greyed outside command post mode, with the reason.
 */
@Composable
fun FreshnessSettingCard(
    state: NetworkFreshnessState,
    editable: Boolean,
    onChange: (FreshnessSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = stringResource(Res.string.freshness_setting_title), style = MaterialTheme.typography.bodyLarge)
            val secondary = MaterialTheme.colorScheme.onSurfaceVariant
            val style = MaterialTheme.typography.bodySmall
            Text(text = stringResource(Res.string.freshness_setting_summary), style = style, color = secondary)
            Text(text = thresholdText(state), style = MaterialTheme.typography.bodyMedium)
            if (!editable) {
                Text(text = stringResource(Res.string.freshness_setting_pc_only), style = style, color = secondary)
            }
        }
        DropDownPreference(
            title = stringResource(Res.string.freshness_setting_interval),
            summary = stringResource(Res.string.freshness_setting_interval_summary),
            enabled = editable,
            items = FreshnessSettings.INTERVAL_CHOICES.map { it to durationLabel(it) },
            selectedItem = settings.expectedInterval,
            onItemSelected = { onChange(settings.copy(expectedInterval = it)) },
        )
        SwitchPreference(
            title = stringResource(Res.string.freshness_setting_auto),
            summary = stringResource(Res.string.freshness_setting_auto_summary),
            checked = settings.autoScale,
            enabled = editable,
            onCheckedChange = { onChange(settings.copy(autoScale = it)) },
        )
        DropDownPreference(
            title = stringResource(Res.string.freshness_setting_fixed),
            summary = stringResource(Res.string.freshness_setting_fixed_summary),
            enabled = editable && !settings.autoScale,
            items = FreshnessSettings.THRESHOLD_CHOICES.map { it to durationLabel(it) },
            selectedItem = settings.fixedThreshold,
            onItemSelected = { onChange(settings.copy(fixedThreshold = it)) },
        )
    }
}

@Composable
private fun thresholdText(state: NetworkFreshnessState): String = if (state.settings.autoScale) {
    stringResource(Res.string.command_post_threshold_auto, durationLabel(state.threshold), state.onlineNodes)
} else {
    stringResource(Res.string.command_post_threshold_fixed, durationLabel(state.threshold))
}

/** "15 min", "1 h 00": the command post view's own words. */
@Composable
private fun durationLabel(duration: Duration): String {
    val minutes = duration.inWholeMinutes
    return if (minutes < MINUTES_PER_HOUR) {
        stringResource(Res.string.command_post_duration_minutes, minutes.toString())
    } else {
        stringResource(
            Res.string.command_post_duration_hours,
            duration.inWholeHours.toString(),
            (minutes % MINUTES_PER_HOUR).toString().padStart(2, '0'),
        )
    }
}
