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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.command_post_duration_days
import org.meshtastic.core.resources.command_post_duration_hours
import org.meshtastic.core.resources.command_post_duration_minutes
import org.meshtastic.core.resources.command_post_duration_seconds
import kotlin.time.Duration

private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

/**
 * NetworkTact: an elapsed time in the command post view's words ("40 s", "18 min", "1 h 05 min", "2 d 3 h"), for the
 * screens outside it (link diagnostic, its banner).
 */
@Composable
fun formatElapsed(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    return when {
        totalMinutes < 1 -> stringResource(Res.string.command_post_duration_seconds, duration.inWholeSeconds.toString())

        totalMinutes < MINUTES_PER_HOUR ->
            stringResource(Res.string.command_post_duration_minutes, totalMinutes.toString())

        duration.inWholeHours < HOURS_PER_DAY ->
            stringResource(
                Res.string.command_post_duration_hours,
                duration.inWholeHours.toString(),
                (totalMinutes % MINUTES_PER_HOUR).toString().padStart(2, '0'),
            )

        else ->
            stringResource(
                Res.string.command_post_duration_days,
                duration.inWholeDays.toString(),
                (duration.inWholeHours % HOURS_PER_DAY).toString(),
            )
    }
}
