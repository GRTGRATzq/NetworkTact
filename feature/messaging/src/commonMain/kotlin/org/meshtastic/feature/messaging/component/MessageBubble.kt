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

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns the [CornerBasedShape] of a message card based on its position in a sequence.
 *
 * The priority bar runs down the start edge of every card, sent or received, so those corners stay square. The end
 * corners are rounded; consecutive cards from the same sender flatten the end corners facing each other (to
 * [groupedCornerRadius]) so a run reads as one visual group.
 *
 * @param cornerRadius The base corner radius of the end edge.
 * @param hasSamePrev Whether the previous message in the list is from the same sender.
 * @param hasSameNext Whether the next message in the list is from the same sender.
 * @param groupedCornerRadius The reduced radius used on corners adjacent to a same-sender neighbor.
 */
fun getMessageCardShape(
    cornerRadius: Dp,
    hasSamePrev: Boolean = false,
    hasSameNext: Boolean = false,
    groupedCornerRadius: Dp = 4.dp,
): CornerBasedShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = if (hasSamePrev) groupedCornerRadius else cornerRadius,
    bottomStart = 0.dp,
    bottomEnd = if (hasSameNext) groupedCornerRadius else cornerRadius,
)
