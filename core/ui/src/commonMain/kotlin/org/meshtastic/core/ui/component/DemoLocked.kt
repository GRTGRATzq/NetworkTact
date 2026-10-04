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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.tactdemo_unavailable
import org.meshtastic.core.ui.theme.AppTheme

/**
 * While [locked], greys [content] out under the caption « Indisponible en mode démo », and nothing in it can be
 * touched, focused or reached by an accessibility service: for settings that act on the real phone or radio directly,
 * which a demo must leave as they are. Scrolling the screen across it still works. Unlocked, [content] is laid out as a
 * plain column with [verticalArrangement], and its state survives the switch either way.
 */
@Composable
fun DemoLocked(
    locked: Boolean,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val caption = stringResource(Res.string.tactdemo_unavailable)
    val lockedModifier = if (locked) Modifier.lockedForDemo(caption) else Modifier
    Box(modifier = modifier.then(lockedModifier).focusGroup()) {
        Column(verticalArrangement = verticalArrangement) {
            if (locked) {
                Text(
                    text = caption,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.alpha(if (locked) DISABLED_ALPHA else 1f),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
        if (locked) {
            // The topmost sibling takes the pointer, so nothing underneath sees a tap; the event is left unconsumed
            // so that the scrolling container around still scrolls.
            Box(modifier = Modifier.matchParentSize().takePointer())
        }
    }
}

/** Out of reach of accessibility services, announced as [caption], and closed to keyboard focus. */
private fun Modifier.lockedForDemo(caption: String): Modifier {
    val silent = clearAndSetSemantics {
        contentDescription = caption
        disabled()
    }
    return silent.focusProperties { onEnter = { cancelFocusChange() } }
}

/** Receives every pointer event without consuming it. */
private fun Modifier.takePointer(): Modifier =
    pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

@PreviewLightDark
@Composable
private fun DemoLockedPreview() {
    AppTheme {
        DemoLocked(locked = true) {
            SwitchPreference(title = "Setting", checked = true, enabled = true, onCheckedChange = {})
        }
    }
}
