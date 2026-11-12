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
package org.meshtastic.feature.messaging.firerequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.Gdh
import org.meshtastic.core.model.firerequest.GdhZone
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.firereq_block_disconnected
import org.meshtastic.core.resources.firereq_block_number
import org.meshtastic.core.resources.firereq_block_recipient
import org.meshtastic.core.resources.firereq_block_reference
import org.meshtastic.core.resources.firereq_block_too_long
import org.meshtastic.core.resources.firereq_direct_note
import org.meshtastic.core.resources.firereq_draft_kept
import org.meshtastic.core.resources.firereq_gdh_invalid
import org.meshtastic.core.resources.firereq_intro
import org.meshtastic.core.resources.firereq_origin_help
import org.meshtastic.core.resources.firereq_recipient_choose
import org.meshtastic.core.resources.firereq_renew
import org.meshtastic.core.resources.firereq_renew_replace
import org.meshtastic.core.resources.firereq_size
import org.meshtastic.core.resources.firereq_title
import org.meshtastic.core.resources.firereq_zone
import org.meshtastic.core.resources.firereq_zone_help
import org.meshtastic.core.resources.replace
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

/**
 * The fire request form, opened from conversation [contactKey]: the sheet box by box, kept as a draft at every change.
 * [bottomBar] holds what goes after the form (its size, then the preview and the sending).
 */
@Composable
fun FireRequestScreen(
    contactKey: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FireRequestViewModel = koinViewModel(),
    bottomBar: @Composable () -> Unit = { FireRequestSizeBar(viewModel) },
) {
    LaunchedEffect(contactKey) { viewModel.start(contactKey) }
    val request by viewModel.request.collectAsStateWithLifecycle()
    val canRenew by viewModel.canRenew.collectAsStateWithLifecycle()
    val zone by viewModel.gdhZone.collectAsStateWithLifecycle()
    val recipient by viewModel.recipient.collectAsStateWithLifecycle()
    val recipients by viewModel.recipients.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel) { FormActions(viewModel::edit, viewModel::chooseZone, viewModel::chooseRecipient) }
    var confirmRenew by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.firereq_title),
                subtitle = stringResource(Res.string.firereq_draft_kept),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {
                    TextButton(
                        enabled = canRenew,
                        onClick = { if (viewModel.hasDraft()) confirmRenew = true else viewModel.renew() },
                    ) {
                        Text(stringResource(Res.string.firereq_renew))
                    }
                },
                onClickChip = {},
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        FireRequestForm(
            request,
            HeaderState(zone, recipient, recipients),
            actions,
            Modifier.fillMaxSize().padding(padding),
        )
    }
    if (confirmRenew) {
        AlertDialog(
            onDismissRequest = { confirmRenew = false },
            text = { Text(stringResource(Res.string.firereq_renew_replace)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRenew = false
                        viewModel.renew()
                    },
                ) {
                    Text(stringResource(Res.string.replace))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRenew = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

/** What the header shows besides the request: the DTG zone, the recipient chosen and the nodes to choose from. */
internal data class HeaderState(
    val zone: GdhZone,
    val recipient: RecipientOption?,
    val recipients: List<RecipientOption>,
)

/** What the form does: change the request, choose the DTG zone, choose the recipient. */
internal class FormActions(
    val edit: ((FireRequest) -> FireRequest) -> Unit,
    val chooseZone: (GdhZone) -> Unit,
    val chooseRecipient: (RecipientOption) -> Unit,
)

@Composable
private fun FireRequestForm(
    request: FireRequest,
    header: HeaderState,
    actions: FormActions,
    modifier: Modifier = Modifier,
) {
    val edit = actions.edit
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Help(stringResource(Res.string.firereq_intro)) }
        item { HeaderCard(request, header, actions) }
        item { NumberBox(request, edit) }
        item { FireTypeBox(request.fireType) { box -> edit { it.copy(fireType = box) } } }
        item { EffectBox(request.effect) { box -> edit { it.copy(effect = box) } } }
        item { ObservationBox(request.observation) { box -> edit { it.copy(observation = box) } } }
        item { ChargeBox(request.charge) { box -> edit { it.copy(charge = box) } } }
        item { TargetsBox(request.targets) { box -> edit { it.copy(targets = box) } } }
        item { LocationBox(request.location) { box -> edit { it.copy(location = box) } } }
        item { ShapeBox(request.shape) { box -> edit { it.copy(shape = box) } } }
        item { AdjustmentBox(request.adjustment) { box -> edit { it.copy(adjustment = box) } } }
        item { FriendsBox(request.friends) { box -> edit { it.copy(friends = box) } } }
        item { CommandBox(request.command) { box -> edit { it.copy(command = box) } } }
    }
}

/** Origin, recipient, request DTG and its format; the two DTGs sent back stay empty here. */
@Composable
private fun HeaderCard(request: FireRequest, header: HeaderState, actions: FormActions) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetField(
                Sheet.ORIGIN,
                request.origin,
                { value -> actions.edit { it.copy(origin = value) } },
                width = 200.dp,
            )
            Help(stringResource(Res.string.firereq_origin_help))
            RecipientChooser(header.recipient, header.recipients, actions.chooseRecipient)
            SheetField(
                Sheet.RECIPIENT,
                request.recipient,
                { value -> actions.edit { it.copy(recipient = value) } },
                width = 200.dp,
                prefilled = true,
            )
            SheetField(
                Sheet.REQUEST_GDH,
                request.requestGdh,
                { value -> actions.edit { it.copy(requestGdh = value) } },
                width = 220.dp,
                prefilled = true,
                filter = { it.take(GDH_MAX) },
                isError = request.requestGdh.isNotBlank() && Gdh.parse(request.requestGdh) == null,
            )
            if (request.requestGdh.isNotBlank() && Gdh.parse(request.requestGdh) == null) {
                Help(stringResource(Res.string.firereq_gdh_invalid), isError = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.firereq_zone))
                GdhZone.entries.forEach { option ->
                    Choice(option.code, header.zone == option) { actions.chooseZone(option) }
                }
            }
            Help(stringResource(Res.string.firereq_zone_help))
            Help(stringResource(Res.string.firereq_direct_note))
        }
    }
}

private const val GDH_MAX = 24

@Composable
private fun RecipientChooser(
    recipient: RecipientOption?,
    options: List<RecipientOption>,
    onChoose: (RecipientOption) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(recipient?.name ?: stringResource(Res.string.firereq_recipient_choose))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        open = false
                        onChoose(option)
                    },
                )
            }
        }
    }
}

/** Under the form: the size the request would be sent as, and why it cannot be sent yet. */
@Composable
fun FireRequestSizeBar(
    viewModel: FireRequestViewModel,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val block by viewModel.sendBlock.collectAsStateWithLifecycle()
    Surface(tonalElevation = 3.dp, modifier = modifier) {
        Column {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.firereq_size, preview.bytes, preview.parts.size))
                    block?.let { Help(sendBlockText(it, preview.parts.size), isError = true) }
                }
                action()
            }
        }
    }
}

@Composable
internal fun sendBlockText(block: SendBlock, parts: Int): String = when (block) {
    SendBlock.NO_NUMBER -> stringResource(Res.string.firereq_block_number)
    SendBlock.NO_RECIPIENT -> stringResource(Res.string.firereq_block_recipient)
    SendBlock.REFERENCE_DIRECTION -> stringResource(Res.string.firereq_block_reference)
    SendBlock.TOO_MANY_PARTS -> stringResource(Res.string.firereq_block_too_long, parts)
    SendBlock.DISCONNECTED -> stringResource(Res.string.firereq_block_disconnected)
}

/** The main button of the bar under the form. */
@Composable
internal fun PrimaryAction(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
