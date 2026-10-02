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
package org.meshtastic.feature.settings.team

import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.team.TeamNameChange
import org.meshtastic.core.model.team.TeamSuffix
import org.meshtastic.proto.User

/** Why the team cannot be written to my radio right now. */
enum class OwnerWriteBlock {
    /** No live link to my radio: nothing is sent, nothing is saved. */
    NOT_CONNECTED,

    /** My radio's own node, or its name, has not been received yet. */
    NO_LOCAL_NODE,

    /** Licensed (ham) mode: the long name is built by the firmware from the call sign and is left alone. */
    LICENSED,
}

/** The reason [ourNode] cannot take a team right now, or null when it can. */
internal fun ownerWriteBlock(connection: ConnectionState, ourNode: Node?): OwnerWriteBlock? = when {
    connection != ConnectionState.Connected -> OwnerWriteBlock.NOT_CONNECTED
    ourNode == null || ourNode.user.long_name.isBlank() -> OwnerWriteBlock.NO_LOCAL_NODE
    ourNode.user.is_licensed -> OwnerWriteBlock.LICENSED
    else -> null
}

/**
 * The owner to send so that my radio declares [team] (or no team when null), and the name change it makes.
 *
 * The result is a copy of [owner] in which only `long_name` differs: `newBuilder()` starts from every field of [owner]
 * (`short_name`, `is_licensed`, `is_unmessagable`, `hw_model`, `public_key`, `id`, unknown fields...) and only
 * `long_name` is set before `build()`.
 */
internal fun ownerWithTeam(owner: User, team: String?): Pair<User, TeamNameChange> {
    val change = TeamSuffix.withTeam(owner.long_name, team)
    return owner.newBuilder().also { it.long_name = change.longName }.build() to change
}
