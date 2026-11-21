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
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.FireRequestDesignation
import org.meshtastic.core.model.firerequest.FireRequestMessage
import org.meshtastic.core.model.firerequest.FireRequestReply
import org.meshtastic.core.model.firerequest.FireRequestThread
import org.meshtastic.core.model.firerequest.ReplyKind
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_allocate
import org.meshtastic.core.resources.firereq_allocate_choose
import org.meshtastic.core.resources.firereq_allocate_done
import org.meshtastic.core.resources.firereq_allocate_refused
import org.meshtastic.core.resources.firereq_allocate_title
import org.meshtastic.core.resources.firereq_no_fire_chief
import org.meshtastic.core.resources.firereq_resend_to
import org.meshtastic.core.resources.firereq_resend_to_note

/** What this phone's post is in the organisation chart, for allocating fire requests. */
enum class FireChiefRole {
    /** No chart, or a chart without a fire chief: allocation is not offered. */
    NONE,

    /** This phone's radio is the fire chief. */
    CHIEF,

    /** Another post is the fire chief. */
    OTHER,
}

/** A radio of the network the fire chief can allocate a request to: its node [id], [shortName] and [longName]. */
data class AllocationRadio(val id: String, val shortName: String, val longName: String) {
    val label: String
        get() = listOf(shortName, longName).filter { it.isNotBlank() }.distinct().joinToString(" · ").ifEmpty { id }
}

/**
 * The texts the fire chief sends when allocating [thread] to [radio] at [gdh], each with the node id it goes to: to the
 * writer, « GDH attribution … · Tir attribué à <radio> », unless the chief wrote the request; to the designated radio,
 * « Tir attribué à vous par <writer> · GDH attribution … », unless it is the writer or the chief's own radio. Every
 * other post of the channel sees nothing: direct messages only.
 */
internal fun allocationMessages(
    thread: FireRequestThread,
    radio: AllocationRadio,
    writerShortName: String,
    gdh: String,
    ourId: String,
): List<Pair<String, String>> = buildList {
    if (!thread.fromLocal) {
        add(
            FireRequestReply(thread.number, ReplyKind.ALLOCATION, gdh, radio.shortName.ifBlank { radio.id }).text() to
                thread.sender,
        )
    }
    val writer = if (thread.fromLocal) ourId else thread.sender
    if (radio.id != writer && radio.id != ourId) {
        add(FireRequestDesignation(thread.number, writerShortName, gdh).text() to radio.id)
    }
}

/** Whether an allocation can go out: [Ready] with its messages, or [Refused] because [nodeId] is not a known radio. */
internal sealed interface AllocationCheck {
    data class Ready(val messages: List<Pair<String, String>>) : AllocationCheck

    data class Refused(val nodeId: String) : AllocationCheck
}

/**
 * [allocationMessages], checked before anything is sent: every message goes to a node id among [knownIds] (the radios
 * of the network), never to a radio found by its short name. One unknown id refuses the whole allocation, so the writer
 * and the designated radio are never told different things.
 */
internal fun checkAllocation(
    thread: FireRequestThread,
    radio: AllocationRadio,
    writerShortName: String,
    gdh: String,
    ourId: String,
    knownIds: Set<String>,
): AllocationCheck {
    if (radio.id !in knownIds) return AllocationCheck.Refused(radio.id)
    val messages = allocationMessages(thread, radio, writerShortName, gdh, ourId)
    val unknown = messages.map { it.second }.firstOrNull { it.isBlank() || it !in knownIds }
    return if (unknown != null) AllocationCheck.Refused(unknown) else AllocationCheck.Ready(messages)
}

/**
 * The allocation this phone made, as shown on its card: the [label] of the designated radio, the [gdh], and the radio
 * that "Renvoyer la fiche à" can reach ([resendTo]).
 */
internal data class MadeAllocation(val label: String, val gdh: String, val resendTo: AllocationRadio?)

/**
 * What [message] (the chief's latest allocation) says, the designated radio identified by its node id only. The message
 * to the designated radio names it by its [FireRequestMessage.to]: an id among [radios] gives its label and the resend,
 * an unknown id is shown as it is, without resend. The message to the writer alone (radio of the writer or of the
 * chief) is shown as sent, without resend: both already hold the sheet.
 */
internal fun madeAllocation(message: FireRequestMessage, radios: List<AllocationRadio>): MadeAllocation? {
    val designation = FireRequestDesignation.parse(message.text)
    return if (designation != null) {
        val radio = radios.firstOrNull { it.id.isNotBlank() && it.id == message.to }
        MadeAllocation(radio?.label ?: message.to.ifBlank { UNKNOWN_RADIO }, designation.gdh, radio)
    } else {
        FireRequestReply.parse(message.text)?.let { reply ->
            reply.designated?.let { MadeAllocation(it, reply.gdh, null) }
        }
    }
}

private const val UNKNOWN_RADIO = "?"

/**
 * The fire chief's part of a card on a channel: « Tir attribué à : » with the menu of the radios and « Attribuer », the
 * allocation made, and « Renvoyer la fiche à <radio> » in direct messages for a radio that is not on the channel. Any
 * other post sees nothing; without a chief in the chart: « Aucun chef de tir dans l'organigramme ».
 */
@Composable
internal fun AllocationPart(
    thread: FireRequestThread,
    role: FireChiefRole,
    allocation: AllocationContext,
    canSend: Boolean,
    actions: FireRequestCardActions,
) {
    when (role) {
        FireChiefRole.OTHER -> return

        FireChiefRole.NONE -> {
            Help(stringResource(Res.string.firereq_no_fire_chief))
            return
        }

        FireChiefRole.CHIEF -> Unit
    }
    var chosen by remember(thread.number) { mutableStateOf<AllocationRadio?>(null) }
    var refused by remember(thread.number) { mutableStateOf<String?>(null) }
    val made = thread.ownAllocation?.let { madeAllocation(it, allocation.radios) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        made?.let {
            Text(
                stringResource(Res.string.firereq_allocate_done, it.label, it.gdh),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.firereq_allocate_title), style = MaterialTheme.typography.labelMedium)
            RadioMenu(chosen, allocation.radios) { chosen = it }
        }
        Button(
            enabled = canSend && chosen != null,
            onClick = {
                val radio = chosen ?: return@Button
                val knownIds = allocation.radios.map { it.id }.toSet()
                val gdh = actions.gdhNow()
                val check = checkAllocation(thread, radio, allocation.writerShortName, gdh, allocation.ourId, knownIds)
                when (check) {
                    is AllocationCheck.Ready -> {
                        check.messages.forEach { (text, to) -> actions.sendDirect(text, to) }
                        chosen = null
                        refused = null
                    }

                    is AllocationCheck.Refused -> refused = check.nodeId.ifBlank { UNKNOWN_RADIO }
                }
            },
        ) {
            Text(stringResource(Res.string.firereq_allocate))
        }
        refused?.let {
            Text(
                stringResource(Res.string.firereq_allocate_refused, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val designated = made?.resendTo
        if (canSend && designated != null) {
            TextButton(onClick = { actions.sendPartsTo(partTexts(thread).map { it.second }, designated.id) }) {
                Text(stringResource(Res.string.firereq_resend_to, designated.shortName.ifBlank { designated.label }))
            }
            Help(stringResource(Res.string.firereq_resend_to_note))
        }
    }
}

/** What allocating needs besides the request: the radios of the network, the writer's short name, this radio's id. */
data class AllocationContext(val radios: List<AllocationRadio>, val writerShortName: String, val ourId: String)

/**
 * What the cards need to know of the network: this post's [role] in the chart, the [radios] to allocate to, and this
 * phone's radio ([ourId], [ourShortName]).
 */
data class FireRequestNetwork(
    val role: FireChiefRole = FireChiefRole.NONE,
    val radios: List<AllocationRadio> = emptyList(),
    val ourId: String = "",
    val ourShortName: String = "",
) {
    /** The allocation context of [thread]: the writer's short name from its first part's sender. */
    fun allocationOf(thread: FireRequestThread, view: FireRequestView): AllocationContext {
        val writer =
            if (thread.fromLocal) {
                ourShortName
            } else {
                thread.parts.values.firstOrNull()?.firstOrNull()?.let { view.messages[it.id] }?.node?.user?.short_name
            }
        return AllocationContext(radios, writer?.ifBlank { null } ?: thread.sender, ourId)
    }
}

@Composable
private fun RadioMenu(chosen: AllocationRadio?, radios: List<AllocationRadio>, onChoose: (AllocationRadio) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(chosen?.label ?: stringResource(Res.string.firereq_allocate_choose))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            radios.forEach { radio ->
                DropdownMenuItem(
                    text = { Text(radio.label) },
                    onClick = {
                        open = false
                        onChoose(radio)
                    },
                )
            }
        }
    }
}
