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

import okio.ByteString.Companion.toByteString
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.proto.Position
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class CommandPostRowsTest {

    private val now = 1_790_000_000L
    private val key = ByteArray(32) { 0x2B.toByte() }.toByteString()

    private fun node(
        num: Int,
        shortName: String,
        lastHeard: Int = 0,
        withKey: Boolean = true,
        fixAgoSeconds: Int? = null,
        isIgnored: Boolean = false,
    ) = Node(
        num = num,
        user =
        User.Builder()
            .also { wb ->
                wb.id = NodeAddress.numToDefaultId(num)
                wb.short_name = shortName
                wb.long_name = shortName
                if (withKey) wb.public_key = key
            }
            .build(),
        lastHeard = lastHeard,
        isIgnored = isIgnored,
        position =
        Position.Builder()
            .also { wb ->
                if (fixAgoSeconds != null) {
                    wb.latitude_i = 488_566_000
                    wb.longitude_i = 23_522_000
                    wb.timestamp = (now - fixAgoSeconds).toInt()
                }
            }
            .build(),
    )

    private val pc = node(num = 100, shortName = "PC-0", lastHeard = now.toInt())

    @Test
    fun `local radio and ignored nodes are left out, most recent first, never heard last`() {
        val rows =
            buildCommandPostRows(
                listOf(
                    node(num = 3, shortName = "CHARLIE-3"),
                    node(num = 1, shortName = "ALPHA-1", lastHeard = (now - 600).toInt()),
                    node(num = 2, shortName = "BRAVO-2", lastHeard = (now - 60).toInt()),
                    node(num = 4, shortName = "DELTA-4", lastHeard = now.toInt(), isIgnored = true),
                    pc,
                ),
                ourNode = pc,
                nowSeconds = now,
            )

        assertEquals(listOf("BRAVO-2", "ALPHA-1", "CHARLIE-3"), rows.map { it.shortName })
        assertEquals(ContactState.NeverHeard, rows.last().contact)
    }

    @Test
    fun `row carries the position state and coordinates only when a position exists`() {
        val rows =
            buildCommandPostRows(
                listOf(
                    node(num = 1, shortName = "ALPHA-1", lastHeard = now.toInt(), fixAgoSeconds = 20 * 60),
                    node(num = 2, shortName = "BRAVO-2", lastHeard = now.toInt()),
                ),
                ourNode = pc,
                nowSeconds = now,
            )

        val alpha = rows.first { it.num == 1 }
        assertEquals(PositionState.Stale(20.minutes), alpha.position)
        assertEquals(48.8566, alpha.latitude!!, 1e-6)

        val bravo = rows.first { it.num == 2 }
        assertEquals(PositionState.NoPosition, bravo.position)
        assertNull(bravo.latitude)
    }

    @Test
    fun `message key matches the existing direct conversation and is absent without a public key`() {
        val rows =
            buildCommandPostRows(
                listOf(
                    node(num = 1, shortName = "ALPHA-1", lastHeard = now.toInt()),
                    node(num = 2, shortName = "BRAVO-2", lastHeard = now.toInt(), withKey = false),
                ),
                ourNode = pc,
                nowSeconds = now,
            )

        assertEquals(
            "${NodeAddress.PKC_CHANNEL_INDEX}${NodeAddress.numToDefaultId(1)}",
            rows.first { it.num == 1 }.directMessageKey,
        )
        assertNull(rows.first { it.num == 2 }.directMessageKey)
    }
}
