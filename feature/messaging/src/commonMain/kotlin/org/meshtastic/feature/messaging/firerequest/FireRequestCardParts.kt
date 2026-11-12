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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import org.meshtastic.core.common.util.nowSeconds
import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.model.firerequest.FireRequestDisplay
import org.meshtastic.core.model.firerequest.FireRequestReply
import org.meshtastic.core.model.firerequest.FireRequestSheet
import org.meshtastic.core.model.firerequest.FireRequestThread
import org.meshtastic.core.model.firerequest.Gdh
import org.meshtastic.core.model.firerequest.LruCoordinate
import org.meshtastic.core.model.firerequest.ReadRequest
import org.meshtastic.core.model.firerequest.ReplyKind
import org.meshtastic.core.repository.FireRequestPrefs
import org.meshtastic.core.repository.SCREEN_DATA
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.coords_show_on_map
import org.meshtastic.core.resources.firereq_close
import org.meshtastic.core.resources.firereq_gdh_invalid
import org.meshtastic.core.resources.firereq_line
import org.meshtastic.core.resources.firereq_missing
import org.meshtastic.core.resources.firereq_not_received
import org.meshtastic.core.resources.firereq_reply_resend
import org.meshtastic.core.resources.firereq_reply_send
import org.meshtastic.core.resources.firereq_reply_title
import org.meshtastic.core.ui.util.MapFocusPoint

/** « GDH attribution » then « GDH fin », each sent back in one short direct message; a second tap resends it. */
@Composable
internal fun ReplyButtons(thread: FireRequestThread, send: (String) -> Unit) {
    var asking by remember { mutableStateOf<ReplyKind?>(null) }
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReplyKind.entries.forEach { kind ->
            val sent = if (kind == ReplyKind.ALLOCATION) thread.allocation != null else thread.end != null
            if (sent) {
                FilledTonalButton(onClick = { asking = kind }) {
                    Text(stringResource(Res.string.firereq_reply_resend, kind.label))
                }
            } else {
                OutlinedButton(onClick = { asking = kind }) { Text(kind.label) }
            }
        }
    }
    asking?.let { kind ->
        ReplyDialog(kind, onDismiss = { asking = null }) { gdh ->
            asking = null
            send(FireRequestReply(thread.number, kind, gdh).text())
        }
    }
}

/** The date-time group to send back, at the phone's time in the zone the operator chose, editable. */
@Composable
private fun ReplyDialog(kind: ReplyKind, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    val prefs = koinInject<FireRequestPrefs>(qualifier = named(SCREEN_DATA))
    var gdh by remember { mutableStateOf(Gdh.of(nowSeconds, prefs.gdhZone.value, systemTimeZone).format()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.firereq_reply_title, kind.label)) },
        text = {
            Column {
                OutlinedTextField(value = gdh, onValueChange = { gdh = it.take(GDH_LENGTH) }, singleLine = true)
                if (Gdh.parse(gdh) == null) Help(stringResource(Res.string.firereq_gdh_invalid), isError = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(gdh) }, enabled = gdh.isNotBlank()) {
                Text(stringResource(Res.string.firereq_reply_send))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

private const val GDH_LENGTH = 24

/** « Partie(s) 3 manquante(s) (cases 9, 10, 11) » while parts are missing. */
@Composable
internal fun MissingParts(thread: FireRequestThread, read: ReadRequest) {
    if (thread.isComplete) return
    val boxes = (1..FireRequestSheet.BOX_COUNT).filterNot { it in read.boxes }.joinToString(", ")
    Help(stringResource(Res.string.firereq_missing, thread.missing.joinToString(", "), boxes), isError = true)
}

/** Boxes 1 to 11 in clear: `non rempli` and `non reçu` in grey, what did not read shown as it came. */
@Composable
internal fun SheetBoxes(read: ReadRequest, showOnMap: ((MapFocusPoint) -> Unit)?) =
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (box in 1..FireRequestSheet.BOX_COUNT) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$box ${FireRequestSheet.BOX_TITLES[box - 1].uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                val lines = if (box == BOX_LOCATION) emptyList() else FireRequestDisplay.lines(read.request, box)
                when {
                    box !in read.boxes -> Grey(stringResource(Res.string.firereq_not_received))
                    box == BOX_LOCATION -> LocationLines(read, showOnMap)
                    lines.isEmpty() && read.unread[box].isNullOrEmpty() -> Grey(FireRequestSheet.NOT_FILLED)
                    else -> lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
                read.unread[box]?.forEach { Text(it, fontFamily = FontFamily.Monospace) }
            }
        }
    }

@Composable
private fun Grey(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Box 7, line by line, with « Voir sur la carte » under each L R U line the converter accepts. */
@Composable
private fun LocationLines(read: ReadRequest, showOnMap: ((MapFocusPoint) -> Unit)?) {
    val entries = FireRequestDisplay.location(read.request.location)
    if (entries.isEmpty() && read.unread[BOX_LOCATION].isNullOrEmpty()) Grey(FireRequestSheet.NOT_FILLED)
    entries.forEach { entry ->
        if (entry.kind != FireRequestSheet.TLE) {
            val heading = entry.letter?.let { stringResource(Res.string.firereq_line, entry.kind, it) } ?: entry.kind
            Text(heading, style = MaterialTheme.typography.labelSmall)
        }
        entry.lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        val line =
            when {
                entry.kind != FireRequestSheet.LRU -> null
                entry.letter == "G" -> read.request.location.g
                else -> read.request.location.h
            }
        val point = line?.let { LruCoordinate.point(it) }
        if (point != null && showOnMap != null) {
            OutlinedButton(
                onClick = { showOnMap(MapFocusPoint(point.latitude, point.longitude, entry.lines.first())) },
            ) {
                Text(stringResource(Res.string.coords_show_on_map))
            }
        }
    }
}

/** « Voir les messages bruts »: every message of the request, as received or sent. */
@Composable
internal fun RawMessagesDialog(thread: FireRequestThread, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                thread.messages.forEach { Text(it.text, fontFamily = FontFamily.Monospace) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.firereq_close)) } },
    )
}
