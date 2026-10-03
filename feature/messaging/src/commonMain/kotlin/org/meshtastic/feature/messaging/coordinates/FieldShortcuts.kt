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
package org.meshtastic.feature.messaging.coordinates

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.coords_converter_title
import org.meshtastic.core.resources.coords_fact_title
import org.meshtastic.core.resources.coords_my_position
import org.meshtastic.core.resources.coords_my_position_none
import org.meshtastic.core.resources.coords_my_position_phone
import org.meshtastic.core.resources.coords_my_position_shortened
import org.meshtastic.core.resources.coords_replace_message
import org.meshtastic.core.resources.replace
import org.meshtastic.core.ui.icon.Map
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.MyLocation
import org.meshtastic.core.ui.icon.Visibility
import org.meshtastic.core.ui.util.SnackbarManager

/**
 * Field shortcuts above the message input. None of them sends anything: they open a tool or fill the input, and the
 * user reviews and sends. Text already typed is only replaced after confirmation.
 *
 * @param canFill whether the input accepts text (it is disabled while the radio is not connected).
 * @param onOpenConverter opens the coordinate converter; null hides that shortcut.
 */
@Composable
internal fun FieldShortcuts(
    messageInputState: TextFieldState,
    canFill: Boolean,
    onOpenConverter: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: FieldShortcutsViewModel = koinViewModel(),
) {
    val snackbarManager = koinInject<SnackbarManager>()
    val coroutineScope = rememberCoroutineScope()
    var pendingText by remember { mutableStateOf<String?>(null) }
    var showObservedFact by remember { mutableStateOf(false) }

    val noPosition = stringResource(Res.string.coords_my_position_none)
    val phoneNote = stringResource(Res.string.coords_my_position_phone)
    val shortenedNote = stringResource(Res.string.coords_my_position_shortened)

    fun fill(text: String) {
        if (messageInputState.text.isBlank()) {
            messageInputState.setTextAndPlaceCursorAtEnd(text)
        } else {
            pendingText = text
        }
    }

    FieldShortcutsRow(
        canFill = canFill,
        onMyPosition = {
            coroutineScope.launch {
                when (val result = viewModel.myPosition()) {
                    MyPositionResult.NoPosition -> snackbarManager.showSnackbar(noPosition)

                    is MyPositionResult.Filled -> {
                        fill(result.text)
                        val notes =
                            listOfNotNull(
                                phoneNote.takeIf { result.fromPhone },
                                shortenedNote.takeIf { result.shortened },
                            )
                        if (notes.isNotEmpty()) snackbarManager.showSnackbar(notes.joinToString(" "))
                    }
                }
            }
        },
        onObservedFact = { showObservedFact = true },
        onOpenConverter = onOpenConverter,
        modifier = modifier,
    )

    if (showObservedFact) {
        ObservedFactDialog(
            onInsert = { text ->
                showObservedFact = false
                fill(text)
            },
            onDismiss = { showObservedFact = false },
        )
    }

    pendingText?.let { text ->
        ReplaceTextDialog(
            onConfirm = {
                messageInputState.setTextAndPlaceCursorAtEnd(text)
                pendingText = null
            },
            onDismiss = { pendingText = null },
        )
    }
}

@Composable
internal fun FieldShortcutsRow(
    canFill: Boolean,
    onMyPosition: () -> Unit,
    onObservedFact: () -> Unit,
    onOpenConverter: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShortcutChip(
            label = stringResource(Res.string.coords_my_position),
            icon = MeshtasticIcons.MyLocation,
            onClick = onMyPosition,
            enabled = canFill,
        )
        ShortcutChip(
            label = stringResource(Res.string.coords_fact_title),
            icon = MeshtasticIcons.Visibility,
            onClick = onObservedFact,
            enabled = canFill,
        )
        if (onOpenConverter != null) {
            ShortcutChip(
                label = stringResource(Res.string.coords_converter_title),
                icon = MeshtasticIcons.Map,
                onClick = onOpenConverter,
            )
        }
    }
}

@Composable
private fun ReplaceTextDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(Res.string.coords_replace_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.replace)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
private fun ShortcutChip(label: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
    )
}
