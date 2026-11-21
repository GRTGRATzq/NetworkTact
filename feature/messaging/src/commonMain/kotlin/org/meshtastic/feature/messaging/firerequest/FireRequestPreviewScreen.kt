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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.utf8Size
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_edit
import org.meshtastic.core.resources.firereq_preview
import org.meshtastic.core.resources.firereq_preview_note
import org.meshtastic.core.resources.firereq_preview_part
import org.meshtastic.core.resources.firereq_preview_part_size
import org.meshtastic.core.resources.firereq_preview_summary
import org.meshtastic.core.resources.firereq_preview_title
import org.meshtastic.core.resources.firereq_send
import org.meshtastic.core.resources.firereq_sending_part
import org.meshtastic.core.resources.firereq_waiting_part
import org.meshtastic.core.ui.component.MainAppBar

/**
 * The fire request, from the form to its sending: the form, then the preview of every part as it will go by radio, then
 * « Envoyer la demande », which sends them all and calls [onShowConversation] with the conversation it went to (the
 * first channel's for « Les deux »).
 */
@Composable
fun FireRequestRoute(
    contactKey: String,
    onNavigateUp: () -> Unit,
    onShowConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FireRequestViewModel = koinViewModel(),
) {
    var previewing by remember { mutableStateOf(false) }
    val block by viewModel.sendBlock.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    if (!previewing) {
        val previewLabel = stringResource(Res.string.firereq_preview)
        // The screen reads the same view model, the one of this navigation entry.
        FireRequestScreen(
            contactKey = contactKey,
            onNavigateUp = onNavigateUp,
            modifier = modifier,
            bottomBar = {
                FireRequestSizeBar(preview, block) { PrimaryAction(previewLabel, block == null, { previewing = true }) }
            },
        )
    } else {
        val request by viewModel.request.collectAsStateWithLifecycle()
        val target by viewModel.target.collectAsStateWithLifecycle()
        val channels by viewModel.channels.collectAsStateWithLifecycle()
        val radios by viewModel.recipients.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        var sending by remember { mutableStateOf(false) }
        val progress by viewModel.progress.collectAsStateWithLifecycle()
        FireRequestPreviewScreen(
            number = request.number,
            preview = preview,
            destination = PreviewDestination(target, channels, radios, viewModel::chooseTarget),
            progress = progress,
            canSend = block == null && !sending,
            onEdit = { previewing = false },
            onSend = {
                sending = true
                scope.launch {
                    val sentTo = viewModel.send()
                    sending = false
                    if (sentTo != null) onShowConversation(sentTo)
                }
            },
            modifier = modifier,
        )
    }
}

/** « Envoyer à » in the preview: the [target] chosen among the [channels] and the [radios], and how to change it. */
class PreviewDestination(
    val target: SendTarget?,
    val channels: SendChannels?,
    val radios: List<RecipientOption>,
    val onChoose: (SendTarget) -> Unit,
)

/**
 * Every part exactly as it goes by radio, with its size, under « Envoyer à » ([destination]); « Modifier » goes back to
 * the form.
 */
@Composable
fun FireRequestPreviewScreen(
    number: String,
    preview: FireRequestPreview,
    destination: PreviewDestination,
    canSend: Boolean,
    onEdit: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    progress: FireRequestProgress? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.firereq_preview_title),
                subtitle = targetSubtitle(destination.target, ""),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onEdit,
                actions = {},
                onClickChip = {},
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column {
                    if (progress != null) {
                        Text(
                            progressText(progress),
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.firereq_edit))
                        }
                        PrimaryAction(stringResource(Res.string.firereq_send), canSend, onSend, Modifier.weight(2f))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TargetChooser(destination.target, destination.channels, destination.radios, destination.onChoose) }
            item { Text(stringResource(Res.string.firereq_preview_summary, number, preview.bytes, preview.parts.size)) }
            itemsIndexed(preview.parts) { index, part -> PartCard(index + 1, preview.parts.size, part) }
            item { Help(stringResource(Res.string.firereq_preview_note)) }
        }
    }
}

/** « Envoi de la partie 2 / 3… », or the part waiting to be sent again after the radio refused it as too fast. */
@Composable
private fun progressText(progress: FireRequestProgress): String = if (progress.waiting) {
    stringResource(Res.string.firereq_waiting_part, progress.part, progress.count, progress.retry)
} else {
    stringResource(Res.string.firereq_sending_part, progress.part, progress.count)
}

@Composable
private fun PartCard(k: Int, count: Int, text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(Res.string.firereq_preview_part, k, count),
                    style = MaterialTheme.typography.labelLarge,
                )
                Help(stringResource(Res.string.firereq_preview_part_size, text.utf8Size()))
            }
            Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
