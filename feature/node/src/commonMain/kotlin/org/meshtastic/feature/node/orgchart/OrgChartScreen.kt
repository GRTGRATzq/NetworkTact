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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.model.freshness.ContactState
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartEdit
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.model.orgchart.OrgMember
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.orgchart_command_post
import org.meshtastic.core.resources.orgchart_empty
import org.meshtastic.core.resources.orgchart_no_member
import org.meshtastic.core.resources.orgchart_revert
import org.meshtastic.core.resources.orgchart_revert_question
import org.meshtastic.core.resources.orgchart_scan
import org.meshtastic.core.resources.orgchart_show_qr
import org.meshtastic.core.resources.orgchart_team
import org.meshtastic.core.resources.orgchart_team_not_in_list
import org.meshtastic.core.resources.orgchart_teams_hint
import org.meshtastic.core.resources.orgchart_title
import org.meshtastic.core.resources.orgchart_unknown_author
import org.meshtastic.core.resources.orgchart_unlisted
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.QrCode
import org.meshtastic.core.ui.icon.QrCodeScanner
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.LocalBarcodeScannerProvider
import org.meshtastic.core.ui.util.LocalBarcodeScannerSupported
import kotlin.time.Duration.Companion.minutes

@Composable
fun OrgChartScreen(
    viewModel: OrgChartViewModel,
    onNavigateUp: () -> Unit,
    onOpenNode: (Int) -> Unit,
    onOpenMessages: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var askRevert by rememberSaveable { mutableStateOf(false) }
    var memberDialog by remember { mutableStateOf<MemberDialogTarget?>(null) }
    var removing by remember { mutableStateOf<OrgMemberRow?>(null) }
    var qrCode by remember { mutableStateOf<QrCodeState?>(null) }
    val editActions =
        remember(state.commandPostMode) {
            if (state.commandPostMode) {
                OrgChartEditActions(
                    onAdd = { team -> memberDialog = MemberDialogTarget(team, index = null) },
                    onEdit = { row ->
                        memberDialog = MemberDialogTarget(row.team, row.index, row.member.callsign, row.member.role)
                    },
                    onMove = { row, up -> viewModel.edit(OrgChartEdit.Move(row.team, row.index, up)) },
                    onRemove = { row -> removing = row },
                )
            } else {
                null
            }
        }
    Scaffold(
        modifier = modifier,
        topBar = {
            OrgChartTopBar(
                commandPostMode = state.commandPostMode,
                onNavigateUp = onNavigateUp,
                onShowQr = { qrCode = viewModel.qrCode() },
                onScanResult = viewModel::onScanned,
            )
        },
    ) { padding ->
        OrgChartContent(
            state = state,
            nowSeconds = nowSeconds,
            onOpenNode = onOpenNode,
            onOpenMessages = onOpenMessages,
            onRevert = { askRevert = true },
            modifier = Modifier.padding(padding),
            editActions = editActions,
        )
    }
    memberDialog?.let { target ->
        MemberDialog(target = target, onSubmit = viewModel::edit, onDismiss = { memberDialog = null })
    }
    removing?.let { row ->
        RemoveMemberDialog(
            row = row,
            onConfirm = {
                viewModel.edit(OrgChartEdit.Remove(row.team, row.index))
                removing = null
            },
            onDismiss = { removing = null },
        )
    }
    qrCode?.let { OrgChartQrDialog(state = it, nowSeconds = nowSeconds, onDismiss = { qrCode = null }) }
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    ScanDialogs(scan = scan, onAdopt = viewModel::adoptScanned, onDismiss = viewModel::clearScan)
    val previous = state.previous
    if (askRevert && previous != null) {
        RevertDialog(
            previous = previous,
            onConfirm = {
                viewModel.revertToPrevious()
                askRevert = false
            },
            onDismiss = { askRevert = false },
        )
    }
}

/** Title, and the QR code (command post) and scanner actions. */
@Composable
private fun OrgChartTopBar(
    commandPostMode: Boolean,
    onNavigateUp: () -> Unit,
    onShowQr: () -> Unit,
    onScanResult: (String?) -> Unit,
) {
    val scanner = LocalBarcodeScannerProvider.current(onScanResult)
    val canScan = LocalBarcodeScannerSupported.current
    MainAppBar(
        title = stringResource(Res.string.orgchart_title),
        ourNode = null,
        showNodeChip = false,
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        onClickChip = {},
        actions = {
            if (commandPostMode) {
                IconButton(onClick = onShowQr) {
                    Icon(MeshtasticIcons.QrCode, contentDescription = stringResource(Res.string.orgchart_show_qr))
                }
            }
            if (canScan) {
                IconButton(onClick = scanner::startScan) {
                    Icon(MeshtasticIcons.QrCodeScanner, contentDescription = stringResource(Res.string.orgchart_scan))
                }
            }
        },
    )
}

/** The answer to a scan: adopt or ignore a chart, or a message when there is nothing to decide. */
@Composable
private fun ScanDialogs(scan: ScanOutcome?, onAdopt: (OrgChart) -> Unit, onDismiss: () -> Unit) {
    when (val outcome = scan) {
        null -> Unit

        is ScanOutcome.Found ->
            AdoptChartDialog(
                chart = outcome.chart,
                nowSeconds = nowSeconds,
                onAdopt = { onAdopt(outcome.chart) },
                onIgnore = onDismiss,
            )

        else -> ScanMessageDialog(outcome = outcome, onDismiss = onDismiss)
    }
}

/** Stateless chart: header, then each section and its members, then the radios no member points to. */
@Composable
fun OrgChartContent(
    state: OrgChartUiState,
    nowSeconds: Long,
    onOpenNode: (Int) -> Unit,
    onOpenMessages: (String) -> Unit,
    onRevert: () -> Unit,
    modifier: Modifier = Modifier,
    editActions: OrgChartEditActions? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { OrgChartHeader(state, nowSeconds, onRevert) }
        if (state.sections.isEmpty()) {
            item { Text(text = stringResource(Res.string.orgchart_empty)) }
        }
        state.sections.forEach { section ->
            item(key = "section_${section.team}") { SectionTitle(section) }
            if (section.members.isEmpty()) {
                item(key = "empty_${section.team}") { SecondaryText(stringResource(Res.string.orgchart_no_member)) }
            }
            items(section.members, key = { "member_${it.member.callsign}" }) { row ->
                MemberCard(row = row, onOpenNode = onOpenNode, onOpenMessages = onOpenMessages) {
                    editActions?.let { MemberActions(row, isLast = row.index == section.members.lastIndex, it) }
                }
            }
            editActions?.let { actions ->
                item(key = "add_${section.team}") { AddMemberButton(onClick = { actions.onAdd(section.team) }) }
            }
        }
        unlisted(state.unlisted, onOpenNode)
    }
}

private fun LazyListScope.unlisted(radios: List<OrgRadioRow>, onOpenNode: (Int) -> Unit) {
    if (radios.isEmpty()) return
    item(key = "unlisted") {
        Text(
            text = stringResource(Res.string.orgchart_unlisted),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
    }
    items(radios, key = { "unlisted_${it.num}" }) { radio -> UnlistedRadioCard(radio, onOpenNode) }
}

@Composable
private fun SectionTitle(section: OrgSection) {
    val title =
        when {
            section.team == null -> stringResource(Res.string.orgchart_command_post)
            section.inTeamList -> stringResource(Res.string.orgchart_team, section.team)
            else -> stringResource(Res.string.orgchart_team_not_in_list, section.team)
        }
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        if (section.team == null) SecondaryText(stringResource(Res.string.orgchart_teams_hint))
    }
}

@Composable
private fun SecondaryText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RevertDialog(previous: OrgChartRecord, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.orgchart_revert)) },
        text = {
            Text(
                stringResource(
                    Res.string.orgchart_revert_question,
                    previous.chart.author.ifBlank { stringResource(Res.string.orgchart_unknown_author) },
                    formatChartTime(previous.chart.createdSeconds, nowSeconds),
                ),
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.orgchart_revert)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** Fictitious chart covering every look: a radio, none, two with one call sign, a team the radio does not declare. */
@PreviewLightDark
@Composable
private fun OrgChartContentPreview() {
    val now = 1_791_720_000L
    fun radio(num: Int, longName: String, matches: Boolean = true, declared: String? = null) = OrgRadioRow(
        num = num,
        longName = longName,
        isMine = num == 0,
        declaredTeam = declared,
        teamMatches = matches,
        contact = ContactState.SeenRecently(2.minutes),
        position = if (num == 3) PositionState.Stale(25.minutes) else PositionState.Fresh(2.minutes),
        directMessageKey = "0!0000000$num",
    )
    fun member(team: String?, rank: Int, callsign: String, role: String, vararg radios: OrgRadioRow) =
        OrgMemberRow(team, rank - 1, rank, OrgMember(callsign, role), radios.toList())
    val state =
        OrgChartUiState(
            record =
            OrgChartRecord(
                OrgChart("PC-0", now - 600, emptyList(), emptyList()),
                OrgChartRecord.Source.SCANNED,
                now,
            ),
            sections =
            listOf(
                OrgSection(null, true, listOf(member(null, 1, "PC-0", "Chef PC", radio(0, "PC-0")))),
                OrgSection(
                    "Alpha",
                    true,
                    listOf(
                        member("Alpha", 1, "ALPHA-1", "Chef d'équipe", radio(1, "ALPHA-1 [Alpha]")),
                        member("Alpha", 2, "CHARLIE-3", "Radio", radio(3, "CHARLIE-3 [Bravo]", false, "Bravo")),
                    ),
                ),
                OrgSection(
                    "Bravo",
                    true,
                    listOf(
                        member("Bravo", 1, "BRAVO-2", "", radio(2, "BRAVO-2 [Bravo]"), radio(5, "BRAVO-2 [Bravo]")),
                        member("Bravo", 2, "DELTA-4", "Éclaireur"),
                    ),
                ),
            ),
        )
    AppTheme { Surface { OrgChartContent(state, now, onOpenNode = {}, onOpenMessages = {}, onRevert = {}) } }
}
