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
package org.meshtastic.feature.messaging.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.tactmsg_alert_icon
import org.meshtastic.core.resources.tactmsg_priority_a11y
import org.meshtastic.core.resources.tactmsg_priority_badge_info
import org.meshtastic.core.resources.tactmsg_priority_badge_report
import org.meshtastic.core.resources.tactmsg_priority_badge_urgent
import org.meshtastic.core.resources.tactmsg_priority_info
import org.meshtastic.core.resources.tactmsg_priority_report
import org.meshtastic.core.resources.tactmsg_priority_selector
import org.meshtastic.core.resources.tactmsg_priority_urgent
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.MIN_GRAPHICAL_CONTRAST
import org.meshtastic.core.ui.theme.MIN_TEXT_CONTRAST
import org.meshtastic.core.ui.theme.TactColors
import org.meshtastic.core.ui.theme.TactHue
import org.meshtastic.core.ui.theme.pickLegible
import org.meshtastic.feature.messaging.priority.MessagePriority

/** Alpha of the priority wash laid over a message bubble: light enough to keep body text at full contrast. */
internal const val PRIORITY_WASH_ALPHA = 0.14f

/**
 * The only colour these screens use to code meaning: red for urgent, amber for a report, the neutral outline for
 * information. Red and amber appear nowhere else in the messaging screens, so they are never ambiguous.
 *
 * Red and amber are the theme's own tones ([TactColors]), held at the 3:1 of a graphical object against the card they
 * edge, the dynamic palette included.
 */
@Composable
internal fun priorityAccent(priority: MessagePriority): Color =
    priorityAccent(priority, CardDefaults.cardColors().containerColor, MaterialTheme.colorScheme.outlineVariant)

/** [priorityAccent] on a given card [background]; [neutral] is the info colour. */
internal fun priorityAccent(priority: MessagePriority, background: Color, neutral: Color): Color = when (priority) {
    MessagePriority.URGENT -> TactColors.legible(TactHue.URGENT, background, MIN_GRAPHICAL_CONTRAST)
    MessagePriority.REPORT -> TactColors.legible(TactHue.REPORT, background, MIN_GRAPHICAL_CONTRAST)
    MessagePriority.INFO -> neutral
}

/** A message card's background: the card colour, under a light wash of the priority's hue for urgent and report. */
@Composable
internal fun priorityCardBackground(priority: MessagePriority): Color {
    val base = CardDefaults.cardColors().containerColor
    return if (priority == MessagePriority.INFO) {
        base
    } else {
        priorityAccent(priority).copy(alpha = PRIORITY_WASH_ALPHA).compositeOver(base)
    }
}

/**
 * Text colour of a card's header line: the text shade of the priority's hue (red for urgent, amber for report, the
 * neutral text colour for info), checked against [background]. A shade that would read under 4.5:1 there, as a
 * system-derived palette could make it, falls back to the neutral text colour.
 */
@Composable
internal fun priorityHeaderColor(priority: MessagePriority, background: Color): Color {
    val scheme = MaterialTheme.colorScheme
    val shade = priorityHeaderShade(priority, darkSurface = scheme.surface.luminance() < DARK_SURFACE_LUMINANCE)
    return pickLegible(
        candidates = listOfNotNull(shade),
        background = background,
        fallback = scheme.onSurface,
        minRatio = MIN_TEXT_CONTRAST,
    )
}

/**
 * The text shade of [priority]'s hue: dark on a light scheme, light on a dark one. Null for info, which keeps the
 * neutral text colour.
 */
internal fun priorityHeaderShade(priority: MessagePriority, darkSurface: Boolean): Color? = when (priority) {
    MessagePriority.URGENT -> if (darkSurface) UrgentTextOnDark else UrgentTextOnLight
    MessagePriority.REPORT -> if (darkSurface) ReportTextOnDark else ReportTextOnLight
    MessagePriority.INFO -> null
}

private val UrgentTextOnLight = Color(0xFF8C1D18)
private val UrgentTextOnDark = Color(0xFFFFB4AB)
private val ReportTextOnLight = Color(0xFF6B4100)
private val ReportTextOnDark = Color(0xFFFFD08A)

/** Luminance below which a scheme's surface is dark; the static schemes sit near the extremes. */
private const val DARK_SURFACE_LUMINANCE = 0.5f

internal fun MessagePriority.badgeRes(): StringResource = when (this) {
    MessagePriority.URGENT -> Res.string.tactmsg_priority_badge_urgent
    MessagePriority.REPORT -> Res.string.tactmsg_priority_badge_report
    MessagePriority.INFO -> Res.string.tactmsg_priority_badge_info
}

private fun MessagePriority.selectorRes(): StringResource = when (this) {
    MessagePriority.URGENT -> Res.string.tactmsg_priority_urgent
    MessagePriority.REPORT -> Res.string.tactmsg_priority_report
    MessagePriority.INFO -> Res.string.tactmsg_priority_info
}

/**
 * The priority's own tag as typed (`[URG]`, `[CR]`), so it reads without colour; info carries none. Screen readers hear
 * the priority's name instead of the brackets.
 */
@Composable
internal fun PriorityTag(priority: MessagePriority, color: Color, modifier: Modifier = Modifier) {
    val tag = priority.prefix ?: return
    val a11y = stringResource(Res.string.tactmsg_priority_a11y, stringResource(priority.selectorRes()))
    Text(
        text = tag,
        modifier = modifier.semantics { contentDescription = a11y },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

/**
 * Info / CR / Urgent choice above the composer. The selection is read back from the text itself, so typing or deleting
 * a prefix by hand keeps the selector truthful, and choosing one only rewrites the prefix.
 */
@Composable
internal fun PrioritySelector(
    selected: MessagePriority,
    onSelect: (MessagePriority) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val description = stringResource(Res.string.tactmsg_priority_selector)
    val options = MessagePriority.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.semantics { contentDescription = description }) {
        options.forEachIndexed { index, priority ->
            val accent = priorityAccent(priority)
            SegmentedButton(
                selected = priority == selected,
                onClick = { onSelect(priority) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                enabled = enabled,
                colors =
                SegmentedButtonDefaults.colors(
                    activeContainerColor = accent.copy(alpha = PRIORITY_WASH_ALPHA * 2),
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    activeBorderColor = accent,
                ),
                label = { Text(stringResource(priority.selectorRes())) },
            )
        }
    }
}

/**
 * High-contrast alert pill for failures, an oversized message and an invalid ack proof: an inverted surface, an alert
 * icon and explicit text. Deliberately neither red (reserved for urgent) nor amber (reserved for reports).
 */
@Composable
internal fun AlertPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = MeshtasticIcons.Warning,
                contentDescription = stringResource(Res.string.tactmsg_alert_icon),
                modifier = Modifier.size(14.dp),
            )
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@PreviewLightDark
@Composable
private fun PrioritySelectorPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessagePriority.entries.forEach { PrioritySelector(selected = it, onSelect = {}) }
                AlertPill(text = "Échec : aucun nœud n’a confirmé la réception")
            }
        }
    }
}
