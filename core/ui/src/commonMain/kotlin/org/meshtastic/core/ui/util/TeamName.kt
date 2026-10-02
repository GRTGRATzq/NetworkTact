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
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.teams_member

/**
 * A node's long name for display, its team suffix moved after the name: `ALPHA-1 [Alpha]` reads `ALPHA-1 · Team Alpha`
 * (`ALPHA-1 · Équipe Alpha` in French). A name without a suffix is returned unchanged.
 *
 * The team is only what the node declares about itself: nothing authenticates it.
 */
@Composable
fun nameWithTeam(longName: String): String {
    val team = TeamSuffix.teamOf(longName) ?: return longName
    return joinNameAndTeam(TeamSuffix.displayName(longName), stringResource(Res.string.teams_member, team))
}

/** `name · team label`, the separator the messaging headers already use. */
fun joinNameAndTeam(name: String, teamLabel: String): String = "$name · $teamLabel"
