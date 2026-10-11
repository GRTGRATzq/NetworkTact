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
package org.meshtastic.feature.node.diagnostic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.diagnostic.BatteryFact
import org.meshtastic.core.model.diagnostic.DiagnosticSuggestion
import org.meshtastic.core.model.diagnostic.HeardCounts
import org.meshtastic.core.model.diagnostic.LastHeard
import org.meshtastic.core.model.diagnostic.LinkDiagnostic
import org.meshtastic.core.model.diagnostic.LoraCheck
import org.meshtastic.core.model.diagnostic.LoraSetting
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.diag_facts
import org.meshtastic.core.resources.diag_intro
import org.meshtastic.core.resources.diag_limits
import org.meshtastic.core.resources.diag_suggest_charge
import org.meshtastic.core.resources.diag_suggest_check_antenna
import org.meshtastic.core.resources.diag_suggest_check_link
import org.meshtastic.core.resources.diag_suggest_check_lora
import org.meshtastic.core.resources.diag_suggest_move_higher
import org.meshtastic.core.resources.diag_suggest_set_region
import org.meshtastic.core.resources.diag_suggestions
import org.meshtastic.core.resources.diag_title
import org.meshtastic.core.ui.component.ConnectionsNavIcon
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.StatusAlertContent
import org.meshtastic.core.ui.theme.statusAlert
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The link diagnostic screen: facts, suggestions, the expected LoRa setting and the manual link test. Nothing on it
 * transmits except the link test, after a confirmation.
 */
@Composable
fun DiagnosticScreen(
    viewModel: DiagnosticViewModel,
    linkTestViewModel: LinkTestViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val testState by linkTestViewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.diag_title),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                onClickChip = {},
            )
        },
    ) { padding ->
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            DiagnosticContent(
                state = current,
                testState = testState,
                onExpectedChange = { viewModel.setExpected(it) },
                onTraceroute = { linkTestViewModel.traceroute(it) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/** Stateless content of [DiagnosticScreen]. */
@Composable
fun DiagnosticContent(
    state: DiagnosticUiState,
    testState: LinkTestUiState,
    onExpectedChange: (LoraSetting?) -> Unit,
    onTraceroute: (TestTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.diag_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FactsCard(state.diagnostic)
        SuggestionsCard(state.diagnostic.suggestions)
        ExpectedLoraCard(
            expected = state.expected,
            localLora = state.localLora,
            commandPostMode = state.commandPostMode,
            onExpectedChange = onExpectedChange,
        )
        LinkTestCard(targets = state.targets, testState = testState, onTraceroute = onTraceroute)
    }
}

@Composable
private fun FactsCard(diagnostic: LinkDiagnostic) {
    DiagnosticCard(title = stringResource(Res.string.diag_facts)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // The radio's state as the navigation bar shows it.
            ConnectionsNavIcon(connectionState = diagnostic.connection, deviceType = null)
            FactLine(linkText(diagnostic.connection), alert = diagnostic.connection != ConnectionState.Connected)
        }
        FactLine(lastPacketText(diagnostic), alert = false)
        FactLine(countsText(diagnostic.counts), alert = false)
        FactLine(lastHeardText(diagnostic.lastHeard, diagnostic.noRecentContact), alert = diagnostic.noRecentContact)
        FactLine(loraText(diagnostic.lora), alert = diagnostic.lora.needsAttention())
        FactLine(batteryText(diagnostic.battery), alert = (diagnostic.battery as? BatteryFact.Level)?.low == true)
    }
}

@Composable
private fun SuggestionsCard(suggestions: List<DiagnosticSuggestion>) {
    DiagnosticCard(title = stringResource(Res.string.diag_suggestions)) {
        suggestions.forEach { suggestion ->
            Text(text = "• " + stringResource(suggestion.text()), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = stringResource(Res.string.diag_limits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A titled card of the diagnostic screen. */
@Composable
internal fun DiagnosticCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

/**
 * One fact. A fact that needs attention is an inverted pill with a warning icon, as the command post view's alerts:
 * never red (urgent messages) nor amber (reports).
 */
@Composable
internal fun FactLine(text: String, alert: Boolean) {
    if (alert) {
        val colorScheme = MaterialTheme.colorScheme
        Row(
            modifier = Modifier.statusAlert(colorScheme),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = MeshtasticIcons.Warning,
                contentDescription = null,
                tint = colorScheme.StatusAlertContent,
                modifier = Modifier.size(16.dp),
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = colorScheme.StatusAlertContent)
        }
    } else {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun LoraCheck.needsAttention(): Boolean = when (this) {
    is LoraCheck.Different -> true
    is LoraCheck.NoExpected -> local.region == LoraSetting.UNSET_REGION
    is LoraCheck.Same -> local.region == LoraSetting.UNSET_REGION
    LoraCheck.NotReceived -> false
}

private fun DiagnosticSuggestion.text(): StringResource = when (this) {
    DiagnosticSuggestion.CHECK_RADIO_LINK -> Res.string.diag_suggest_check_link
    DiagnosticSuggestion.CHECK_LORA_SETTINGS -> Res.string.diag_suggest_check_lora
    DiagnosticSuggestion.SET_REGION -> Res.string.diag_suggest_set_region
    DiagnosticSuggestion.CHARGE_BATTERY -> Res.string.diag_suggest_charge
    DiagnosticSuggestion.MOVE_HIGHER -> Res.string.diag_suggest_move_higher
    DiagnosticSuggestion.CHECK_ANTENNA -> Res.string.diag_suggest_check_antenna
}

/** Fictitious data: BRAVO-2 last heard 18 min ago, LoRa different from the expected setting, low battery. */
@PreviewLightDark
@Composable
private fun DiagnosticContentPreview() {
    val nowMillis = 1_800_000_000_000L
    val expected = LoraSetting("EU_868", "LONG_FAST")
    val local = LoraSetting("EU_868", "MEDIUM_FAST")
    val diagnostic =
        LinkDiagnostic(
            connection = ConnectionState.Connected,
            lastRadioPacketAge = 40.seconds,
            lastRadioPacketMillis = nowMillis - 40_000,
            counts = HeardCounts(0, 0, 2),
            lastHeard = LastHeard("BRAVO-2", nowMillis / 1000 - 18 * 60, 18.minutes),
            noRecentContact = true,
            lora = LoraCheck.Different(local, expected, regionDiffers = false, presetDiffers = true),
            battery = BatteryFact.Level(15, low = true),
            suggestions =
            listOf(
                DiagnosticSuggestion.CHECK_LORA_SETTINGS,
                DiagnosticSuggestion.CHARGE_BATTERY,
                DiagnosticSuggestion.MOVE_HIGHER,
                DiagnosticSuggestion.CHECK_ANTENNA,
            ),
        )
    val state =
        DiagnosticUiState(
            diagnostic = diagnostic,
            localLora = local,
            expected = expected,
            commandPostMode = true,
            targets = listOf(TestTarget(2, "BRAVO-2", 0), TestTarget(3, "CHARLIE-3", 0)),
        )
    AppTheme {
        Surface {
            DiagnosticContent(
                state = state,
                testState = LinkTestUiState(connected = true, demo = false, waitSeconds = 0, lastSent = null),
                onExpectedChange = {},
                onTraceroute = {},
            )
        }
    }
}
