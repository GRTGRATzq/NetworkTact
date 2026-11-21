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
import org.meshtastic.core.model.firerequest.FireRequestReply
import org.meshtastic.core.model.firerequest.FireRequestThread
import org.meshtastic.core.model.firerequest.ReplyKind
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_allocate
import org.meshtastic.core.resources.firereq_allocate_choose
import org.meshtastic.core.resources.firereq_allocate_done
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
    val made =
        thread.ownAllocation?.let { message ->
            val designated = allocation.radios.firstOrNull { it.id == message.to }
            val gdh =
                FireRequestDesignation.parse(message.text)?.gdh ?: FireRequestReply.parse(message.text)?.gdh.orEmpty()
            val named = FireRequestReply.parse(message.text)?.designated
            (designated ?: allocation.radios.firstOrNull { it.shortName == named }) to gdh
        }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        made?.let { (radio, gdh) ->
            Text(
                stringResource(Res.string.firereq_allocate_done, radio?.label ?: "?", gdh),
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
                allocationMessages(thread, radio, allocation.writerShortName, actions.gdhNow(), allocation.ourId)
                    .forEach { (text, to) -> actions.sendDirect(text, to) }
                chosen = null
            },
        ) {
            Text(stringResource(Res.string.firereq_allocate))
        }
        val designated = made?.first
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
