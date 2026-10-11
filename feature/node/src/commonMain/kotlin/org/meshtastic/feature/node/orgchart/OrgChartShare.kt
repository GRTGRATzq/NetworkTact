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
package org.meshtastic.feature.node.orgchart

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartCode
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.okay
import org.meshtastic.core.resources.orgchart_adopt
import org.meshtastic.core.resources.orgchart_adopt_title
import org.meshtastic.core.resources.orgchart_adopted
import org.meshtastic.core.resources.orgchart_command_post
import org.meshtastic.core.resources.orgchart_empty
import org.meshtastic.core.resources.orgchart_error_too_large
import org.meshtastic.core.resources.orgchart_ignore
import org.meshtastic.core.resources.orgchart_no_auth
import org.meshtastic.core.resources.orgchart_qr_hint
import org.meshtastic.core.resources.orgchart_scan_invalid
import org.meshtastic.core.resources.orgchart_scan_same
import org.meshtastic.core.resources.orgchart_show_qr
import org.meshtastic.core.resources.orgchart_summary
import org.meshtastic.core.resources.orgchart_summary_line
import org.meshtastic.core.resources.orgchart_team
import org.meshtastic.core.ui.util.QrErrorCorrection
import org.meshtastic.core.ui.util.SetScreenBrightness
import org.meshtastic.core.ui.util.rememberQrCodePainter
import org.meshtastic.feature.node.commandpost.FreshnessLine
import org.meshtastic.feature.node.commandpost.FreshnessTone

/**
 * The chart's QR code, for the other phones to scan, with who made it and when. Shown as wide as the screen allows and
 * at full brightness, so a camera reads it from another phone; nothing goes over the radio.
 */
@Composable
internal fun OrgChartQrDialog(state: QrCodeState, nowSeconds: Long, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(Res.string.orgchart_show_qr)) },
        text = {
            when (state) {
                is QrCodeState.Ready -> QrCodeContent(state, nowSeconds)

                is QrCodeState.TooLarge ->
                    FreshnessLine(
                        text =
                        stringResource(
                            Res.string.orgchart_error_too_large,
                            state.length,
                            OrgChartCode.MAX_CODE_CHARS,
                        ),
                        tone = FreshnessTone.ALERT,
                    )

                QrCodeState.Empty -> Text(stringResource(Res.string.orgchart_empty))
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(Res.string.okay)) } },
    )
}

@Composable
private fun QrCodeContent(state: QrCodeState.Ready, nowSeconds: Long) {
    SetScreenBrightness(1f)
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(chartOrigin(state.chart, nowSeconds), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(Res.string.orgchart_summary, state.chart.teams.size, state.chart.memberCount),
            style = MaterialTheme.typography.bodySmall,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Rendered at the size it is shown, one bitmap pixel per screen pixel.
            val sizePx = with(LocalDensity.current) { maxWidth.roundToPx() }
            Image(
                painter = rememberQrCodePainter(state.code, sizePx, QrErrorCorrection.MEDIUM),
                contentDescription = stringResource(Res.string.orgchart_show_qr),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).testTag("orgchart_qr"),
            )
        }
        Text(stringResource(Res.string.orgchart_qr_hint), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Asks before adopting a scanned chart: who made it and when, what it holds, and that nothing proves where it comes
 * from. Nothing is adopted without an answer.
 */
@Composable
internal fun AdoptChartDialog(chart: OrgChart, nowSeconds: Long, onAdopt: () -> Unit, onIgnore: () -> Unit) {
    AlertDialog(
        onDismissRequest = onIgnore,
        title = { Text(stringResource(Res.string.orgchart_adopt_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(chartOrigin(chart, nowSeconds), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(Res.string.orgchart_summary, chart.teams.size, chart.memberCount))
                if (chart.commandPost.isNotEmpty()) {
                    SummaryLine(stringResource(Res.string.orgchart_command_post), chart.commandPost.map { it.callsign })
                }
                chart.teams.forEach { team ->
                    SummaryLine(stringResource(Res.string.orgchart_team, team.name), team.members.map { it.callsign })
                }
                Text(
                    text = stringResource(Res.string.orgchart_no_auth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onAdopt) { Text(stringResource(Res.string.orgchart_adopt)) } },
        dismissButton = { TextButton(onClick = onIgnore) { Text(stringResource(Res.string.orgchart_ignore)) } },
    )
}

@Composable
private fun SummaryLine(title: String, callsigns: List<String>) {
    Text(
        text = stringResource(Res.string.orgchart_summary_line, title, callsigns.joinToString(", ")),
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** The outcome of a scan that needs no decision: not a chart, the same chart, or adopted. */
@Composable
internal fun ScanMessageDialog(outcome: ScanOutcome, onDismiss: () -> Unit) {
    val text =
        when (outcome) {
            ScanOutcome.Invalid -> stringResource(Res.string.orgchart_scan_invalid)
            ScanOutcome.Same -> stringResource(Res.string.orgchart_scan_same)
            ScanOutcome.Adopted -> stringResource(Res.string.orgchart_adopted)
            is ScanOutcome.Found -> return
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(Res.string.okay)) } },
    )
}
