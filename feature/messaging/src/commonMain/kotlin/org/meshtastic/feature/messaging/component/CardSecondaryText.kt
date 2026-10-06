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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.meshtastic.core.ui.theme.MIN_TEXT_CONTRAST
import org.meshtastic.core.ui.theme.pickLegible

/**
 * Gives [content] a secondary text colour that reads at 4.5:1 on [background]: the scheme's own when it does, the main
 * text colour otherwise. On the light card and its priority wash, the scheme's secondary colour falls short.
 */
@Composable
internal fun LegibleSecondaryContent(background: Color, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val secondary = legibleSecondary(scheme.onSurfaceVariant, scheme.onSurface, background)
    if (secondary == scheme.onSurfaceVariant) {
        content()
    } else {
        MaterialTheme(colorScheme = scheme.copy(onSurfaceVariant = secondary), content = content)
    }
}

/** [secondary] if it reads at 4.5:1 on [background], else [primary]. */
internal fun legibleSecondary(secondary: Color, primary: Color, background: Color): Color = pickLegible(
    candidates = listOf(secondary),
    background = background,
    fallback = primary,
    minRatio = MIN_TEXT_CONTRAST,
)
