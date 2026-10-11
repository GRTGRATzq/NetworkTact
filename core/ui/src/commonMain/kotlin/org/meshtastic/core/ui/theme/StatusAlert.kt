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
package org.meshtastic.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// NetworkTact: a status that needs attention (no usable signal, battery under 20 %, mismatched key, etc.) is drawn in
// inverted colours, like the alert pills of the messages, never in red (urgent messages) nor amber (reports). The
// inverted surface gives the same contrast as the normal text in both themes.
private val StatusAlertShape = RoundedCornerShape(4.dp)

/** Colour of an icon or text drawn on [statusAlert]'s background. */
val ColorScheme.StatusAlertContent: Color
    get() = inverseOnSurface

/** The inverted, rounded background of a status that needs attention, when [active]. */
fun Modifier.statusAlert(colorScheme: ColorScheme, active: Boolean = true): Modifier = if (active) {
    background(colorScheme.inverseSurface, StatusAlertShape).padding(horizontal = 3.dp, vertical = 1.dp)
} else {
    this
}
