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
package org.meshtastic.feature.node.commandpost

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.freshness.ContactState
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.theme.MIN_TEXT_CONTRAST
import org.meshtastic.core.ui.theme.TactColors
import org.meshtastic.core.ui.theme.TactHue

/**
 * How a contact or position line of the command post view looks. Red and amber are reserved for message priorities, so
 * an alarming state is an inverted pill with a warning icon, as the messaging alerts are, never a hue.
 */
internal enum class FreshnessTone {
    /** Nothing to report or nothing known: secondary text. */
    NEUTRAL,

    /** A recent contact: main text. */
    NORMAL,

    /** A position whose fix time comes from the sender and is recent: green. */
    FRESH,

    /** An old position or a clock ahead of ours: inverted pill. */
    ALERT,
}

internal fun ContactState.tone(): FreshnessTone = when (this) {
    ContactState.NeverHeard -> FreshnessTone.NEUTRAL
    is ContactState.SeenRecently -> FreshnessTone.NORMAL
    is ContactState.NotHeardSince -> FreshnessTone.NEUTRAL
    is ContactState.InconsistentTimestamp -> FreshnessTone.ALERT
}

/** Only a fix time from the sender earns the fresh colour; every other state stays neutral or alarming. */
internal fun PositionState.tone(): FreshnessTone = when (this) {
    PositionState.NoPosition -> FreshnessTone.NEUTRAL
    is PositionState.Fresh -> FreshnessTone.FRESH
    is PositionState.Stale -> FreshnessTone.ALERT
    is PositionState.StaleAtLeast -> FreshnessTone.ALERT
    is PositionState.ReceivedFixTimeUnknown -> FreshnessTone.NEUTRAL
    PositionState.NoFixTime -> FreshnessTone.NEUTRAL
    is PositionState.InconsistentTimestamp -> FreshnessTone.ALERT
}

/**
 * Green of a fresh position on [background]: the theme's own tone at 4.5:1, or, if a dynamic palette puts the card
 * where none reads, the fixed green with the most contrast.
 */
internal fun freshColor(background: Color): Color = TactColors.legible(TactHue.FRESH, background, MIN_TEXT_CONTRAST)

@Composable
internal fun FreshnessLine(text: String, tone: FreshnessTone) {
    val scheme = MaterialTheme.colorScheme
    when (tone) {
        FreshnessTone.ALERT -> FreshnessAlert(text)

        FreshnessTone.FRESH ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = freshColor(CardDefaults.cardColors().containerColor),
            )

        FreshnessTone.NORMAL -> Text(text = text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)

        FreshnessTone.NEUTRAL ->
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun FreshnessAlert(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(MeshtasticIcons.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
