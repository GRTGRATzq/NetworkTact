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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.orgchart.EditResult
import org.meshtastic.core.model.orgchart.MemberInput
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartCode
import org.meshtastic.core.model.orgchart.OrgChartEdit
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.okay
import org.meshtastic.core.resources.orgchart_add_member
import org.meshtastic.core.resources.orgchart_callsign
import org.meshtastic.core.resources.orgchart_callsign_hint
import org.meshtastic.core.resources.orgchart_edit_member
import org.meshtastic.core.resources.orgchart_error_callsign_long
import org.meshtastic.core.resources.orgchart_error_duplicate
import org.meshtastic.core.resources.orgchart_error_empty
import org.meshtastic.core.resources.orgchart_error_role_long
import org.meshtastic.core.resources.orgchart_error_too_large
import org.meshtastic.core.resources.orgchart_move_down
import org.meshtastic.core.resources.orgchart_move_up
import org.meshtastic.core.resources.orgchart_remove_member
import org.meshtastic.core.resources.orgchart_remove_question
import org.meshtastic.core.resources.orgchart_role
import org.meshtastic.core.ui.icon.Add
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.Edit
import org.meshtastic.core.ui.icon.KeyboardArrowDown
import org.meshtastic.core.ui.icon.KeyboardArrowUp
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.node.commandpost.FreshnessLine
import org.meshtastic.feature.node.commandpost.FreshnessTone

/** What the command post can do on its chart; null on the other phones, which only read it. */
class OrgChartEditActions(
    val onAdd: (team: String?) -> Unit,
    val onEdit: (OrgMemberRow) -> Unit,
    val onMove: (OrgMemberRow, up: Boolean) -> Unit,
    val onRemove: (OrgMemberRow) -> Unit,
)

/** Which member the member dialog adds ([index] null) or edits, in which section. */
data class MemberDialogTarget(val team: String?, val index: Int?, val callsign: String = "", val role: String = "")

/** Up, down, edit and remove, under a member's card. */
@Composable
internal fun MemberActions(
    row: OrgMemberRow,
    isLast: Boolean,
    actions: OrgChartEditActions,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = { actions.onMove(row, true) }, enabled = row.index > 0) {
            Icon(MeshtasticIcons.KeyboardArrowUp, contentDescription = stringResource(Res.string.orgchart_move_up))
        }
        IconButton(onClick = { actions.onMove(row, false) }, enabled = !isLast) {
            Icon(MeshtasticIcons.KeyboardArrowDown, contentDescription = stringResource(Res.string.orgchart_move_down))
        }
        IconButton(onClick = { actions.onEdit(row) }) {
            Icon(MeshtasticIcons.Edit, contentDescription = stringResource(Res.string.orgchart_edit_member))
        }
        IconButton(onClick = { actions.onRemove(row) }) {
            Icon(MeshtasticIcons.Delete, contentDescription = stringResource(Res.string.orgchart_remove_member))
        }
    }
}

@Composable
internal fun AddMemberButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(MeshtasticIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = stringResource(Res.string.orgchart_add_member), modifier = Modifier.padding(start = 6.dp))
    }
}

/**
 * Adds or edits a member. [onSubmit] applies the change and returns how it went: the dialog closes when it is done, and
 * otherwise says why it was refused, including when the chart would no longer fit in a readable QR code.
 */
@Composable
internal fun MemberDialog(target: MemberDialogTarget, onSubmit: (OrgChartEdit) -> EditResult, onDismiss: () -> Unit) {
    var callsign by rememberSaveable(target) { mutableStateOf(target.callsign) }
    var role by rememberSaveable(target) { mutableStateOf(target.role) }
    var result by remember(target) { mutableStateOf<EditResult?>(null) }
    val error = result?.let { editError(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (target.index == null) Res.string.orgchart_add_member else Res.string.orgchart_edit_member,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = callsign,
                    onValueChange = {
                        callsign = it
                        result = null
                    },
                    label = { Text(stringResource(Res.string.orgchart_callsign)) },
                    supportingText = { Text(stringResource(Res.string.orgchart_callsign_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("orgchart_callsign"),
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = {
                        role = it
                        result = null
                    },
                    label = { Text(stringResource(Res.string.orgchart_role)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("orgchart_role"),
                )
                // Inverted pill, as every alert of the app: red stays reserved for urgent messages.
                error?.let { FreshnessLine(text = it, tone = FreshnessTone.ALERT) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val edit =
                        if (target.index == null) {
                            OrgChartEdit.Add(target.team, callsign, role)
                        } else {
                            OrgChartEdit.Update(target.team, target.index, callsign, role)
                        }
                    val outcome = onSubmit(edit)
                    if (outcome is EditResult.Changed || outcome is EditResult.Unchanged) {
                        onDismiss()
                    } else {
                        result = outcome
                    }
                },
            ) {
                Text(stringResource(Res.string.okay))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** Why an edit was refused, in the user's words; null when it went through. */
@Composable
internal fun editError(result: EditResult): String? = when (result) {
    is EditResult.TooLarge ->
        stringResource(Res.string.orgchart_error_too_large, result.length, OrgChartCode.MAX_CODE_CHARS)

    is EditResult.Invalid ->
        when (val reason = result.reason) {
            MemberInput.EmptyCallsign -> stringResource(Res.string.orgchart_error_empty)

            is MemberInput.CallsignTooLong ->
                stringResource(Res.string.orgchart_error_callsign_long, reason.bytes, OrgChart.MAX_CALLSIGN_BYTES)

            is MemberInput.RoleTooLong ->
                stringResource(Res.string.orgchart_error_role_long, reason.bytes, OrgChart.MAX_ROLE_BYTES)

            is MemberInput.DuplicateCallsign -> stringResource(Res.string.orgchart_error_duplicate, reason.callsign)

            is MemberInput.Valid -> null
        }

    is EditResult.Changed,
    EditResult.Unchanged,
    -> null
}

@Composable
internal fun RemoveMemberDialog(row: OrgMemberRow, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.orgchart_remove_member)) },
        text = { Text(stringResource(Res.string.orgchart_remove_question, row.member.callsign)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.orgchart_remove_member)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
