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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.message
import org.meshtastic.core.resources.orgchart_declares_no_team
import org.meshtastic.core.resources.orgchart_declares_team
import org.meshtastic.core.resources.orgchart_my_radio
import org.meshtastic.core.resources.orgchart_no_radio
import org.meshtastic.core.resources.orgchart_origin
import org.meshtastic.core.resources.orgchart_read_only
import org.meshtastic.core.resources.orgchart_revert
import org.meshtastic.core.resources.orgchart_several_radios
import org.meshtastic.core.resources.orgchart_source_edited
import org.meshtastic.core.resources.orgchart_source_scanned
import org.meshtastic.core.resources.orgchart_summary
import org.meshtastic.core.resources.orgchart_unknown_author
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Message
import org.meshtastic.core.ui.util.nameWithTeam
import org.meshtastic.feature.node.commandpost.ContactLine
import org.meshtastic.feature.node.commandpost.PositionLine

private const val MILLIS_PER_SECOND = 1000L
private const val SAME_DAY_SECONDS = 12 * 60 * 60L

/** A time of the chart: the clock time when recent, the date and time otherwise. */
internal fun formatChartTime(seconds: Long, nowSeconds: Long): String =
    if (nowSeconds - seconds in 0..SAME_DAY_SECONDS) {
        DateFormatter.formatTime(seconds * MILLIS_PER_SECOND)
    } else {
        DateFormatter.formatDateTimeShort(seconds * MILLIS_PER_SECOND)
    }

/** "Org chart of PC-0, created at 14:05". */
@Composable
internal fun chartOrigin(chart: OrgChart, nowSeconds: Long): String = stringResource(
    Res.string.orgchart_origin,
    chart.author.ifBlank { stringResource(Res.string.orgchart_unknown_author) },
    formatChartTime(chart.createdSeconds, nowSeconds),
)

/** Who made the chart in use and when, where this phone got it, and the way back to the previous one. */
@Composable
internal fun OrgChartHeader(state: OrgChartUiState, nowSeconds: Long, onRevert: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        state.record?.let { record ->
            Text(chartOrigin(record.chart, nowSeconds), style = MaterialTheme.typography.titleSmall)
            val chart = record.chart
            Text(
                text =
                stringResource(Res.string.orgchart_summary, chart.teams.size, chart.memberCount) +
                    " · " +
                    when (record.source) {
                        OrgChartRecord.Source.EDITED -> stringResource(Res.string.orgchart_source_edited)

                        OrgChartRecord.Source.SCANNED ->
                            stringResource(
                                Res.string.orgchart_source_scanned,
                                formatChartTime(record.timeSeconds, nowSeconds),
                            )
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!state.commandPostMode) {
            Text(
                text = stringResource(Res.string.orgchart_read_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.previous != null) {
            TextButton(onClick = onRevert, modifier = Modifier.testTag("orgchart_revert")) {
                Text(stringResource(Res.string.orgchart_revert))
            }
        }
    }
}

/**
 * A member: rank, call sign and role, then the radio carrying its call sign with its last contact and position age, as
 * in the command post view. Without such a radio it says so; with several, each is listed and none is picked.
 */
@Composable
internal fun MemberCard(
    row: OrgMemberRow,
    onOpenNode: (Int) -> Unit,
    onOpenMessages: (String) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val single = row.radios.singleOrNull()
    Card(
        modifier =
        modifier.fillMaxWidth().testTag("orgchart_member_${row.member.callsign}").clickable(
            enabled = single != null,
        ) {
            single?.let { onOpenNode(it.num) }
        },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${row.rank}. ${row.member.callsign}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (row.member.role.isNotBlank()) {
                        Text(text = row.member.role, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                single?.directMessageKey?.let { key -> MessageButton { onOpenMessages(key) } }
            }
            when {
                single != null -> RadioLines(single)

                row.radios.isEmpty() ->
                    Text(
                        text = stringResource(Res.string.orgchart_no_radio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                else -> SeveralRadios(row.radios, onOpenNode, onOpenMessages)
            }
            actions()
        }
    }
}

@Composable
private fun SeveralRadios(radios: List<OrgRadioRow>, onOpenNode: (Int) -> Unit, onOpenMessages: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.orgchart_several_radios, radios.size),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        radios.forEach { radio ->
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenNode(radio.num) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioLines(radio, modifier = Modifier.weight(1f))
                radio.directMessageKey?.let { key -> MessageButton { onOpenMessages(key) } }
            }
        }
    }
}

/** A radio's name, last contact and position age, and whether its own name declares the member's team. */
@Composable
internal fun RadioLines(radio: OrgRadioRow, modifier: Modifier = Modifier) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val name = nameWithTeam(radio.longName)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (radio.isMine) "$name · ${stringResource(Res.string.orgchart_my_radio)}" else name,
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
        )
        ContactLine(radio.contact)
        PositionLine(radio.position)
        if (!radio.teamMatches) {
            Text(
                text =
                radio.declaredTeam?.let { stringResource(Res.string.orgchart_declares_team, it) }
                    ?: stringResource(Res.string.orgchart_declares_no_team),
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
            )
        }
    }
}

/** A radio no member's call sign points to. */
@Composable
internal fun UnlistedRadioCard(radio: OrgRadioRow, onOpenNode: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenNode(radio.num) }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = nameWithTeam(radio.longName),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            ContactLine(radio.contact)
        }
    }
}

@Composable
private fun MessageButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(MeshtasticIcons.Message, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = stringResource(Res.string.message), modifier = Modifier.padding(start = 6.dp))
    }
}
