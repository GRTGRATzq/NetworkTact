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

import org.meshtastic.core.model.Node
import org.meshtastic.proto.Position
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class NodeFreshnessTest {

    private val now = 1_790_000_000L
    private val t = FreshnessThresholds.Default

    private fun ago(seconds: Long): Int = (now - seconds).toInt()

    private fun user(id: String, longName: String, shortName: String) = User.Builder()
        .also { wb ->
            wb.id = id
            wb.long_name = longName
            wb.short_name = shortName
        }
        .build()

    private fun position(timestamp: Int = 0, time: Int = 0, hasPosition: Boolean = true) =
        NodeFreshness.position(hasPosition, timestamp, time, now, t)

    // region thresholds

    @Test
    fun `default thresholds match the field requirements`() {
        assertEquals(15.minutes, t.recentContact)
        assertEquals(10.minutes, t.stalePosition)
        assertEquals(2.minutes, t.clockTolerance)
        assertEquals(15.seconds, t.refreshInterval)
    }

    // endregion

    // region contact

    @Test
    fun `lastHeard zero is never heard`() {
        assertEquals(ContactState.NeverHeard, NodeFreshness.contact(0, now, t))
    }

    @Test
    fun `contact exactly at the recent threshold is still recent`() {
        assertEquals(ContactState.SeenRecently(15.minutes), NodeFreshness.contact(ago(15 * 60), now, t))
    }

    @Test
    fun `contact one second past the recent threshold is not heard since`() {
        assertEquals(
            ContactState.NotHeardSince(15.minutes + 1.seconds),
            NodeFreshness.contact(ago(15 * 60 + 1), now, t),
        )
    }

    @Test
    fun `contact slightly in the future within tolerance counts as just seen`() {
        assertEquals(ContactState.SeenRecently(0.seconds), NodeFreshness.contact(ago(-120), now, t))
    }

    @Test
    fun `contact beyond the clock tolerance is inconsistent`() {
        assertEquals(ContactState.InconsistentTimestamp(121.seconds), NodeFreshness.contact(ago(-121), now, t))
    }

    // endregion

    // region position with the sender's fix time

    @Test
    fun `no valid position wins over any time`() {
        assertEquals(PositionState.NoPosition, position(timestamp = ago(10), time = ago(10), hasPosition = false))
    }

    @Test
    fun `fix time under the threshold is fresh`() {
        assertEquals(PositionState.Fresh(5.minutes), position(timestamp = ago(5 * 60)))
    }

    @Test
    fun `fix time exactly at the threshold is still fresh`() {
        assertEquals(PositionState.Fresh(10.minutes), position(timestamp = ago(10 * 60)))
    }

    @Test
    fun `fix time past the threshold is old`() {
        assertEquals(PositionState.Stale(10.minutes + 1.seconds), position(timestamp = ago(10 * 60 + 1)))
    }

    @Test
    fun `fix time is preferred over the stored time`() {
        // A recent reception must not make an old fix look fresh.
        assertEquals(PositionState.Stale(60.minutes), position(timestamp = ago(3600), time = ago(5)))
    }

    @Test
    fun `fix time in the future within tolerance is fresh with zero age`() {
        assertEquals(PositionState.Fresh(0.seconds), position(timestamp = ago(-120)))
    }

    @Test
    fun `fix time beyond the clock tolerance is inconsistent`() {
        assertEquals(PositionState.InconsistentTimestamp(121.seconds), position(timestamp = ago(-121)))
    }

    // endregion

    // region position without the sender's fix time

    @Test
    fun `without fix time a recent time is never fresh`() {
        assertEquals(PositionState.ReceivedFixTimeUnknown(5.minutes), position(time = ago(5 * 60)))
    }

    @Test
    fun `without fix time exactly at the threshold stays unknown`() {
        assertEquals(PositionState.ReceivedFixTimeUnknown(10.minutes), position(time = ago(10 * 60)))
    }

    @Test
    fun `without fix time past the threshold is certainly old`() {
        assertEquals(PositionState.StaleAtLeast(10.minutes + 1.seconds), position(time = ago(10 * 60 + 1)))
    }

    @Test
    fun `without any time the age is unknown and not estimated`() {
        assertEquals(PositionState.NoFixTime, position())
    }

    @Test
    fun `without fix time a time beyond the clock tolerance is inconsistent`() {
        assertEquals(PositionState.InconsistentTimestamp(5.minutes), position(time = ago(-5 * 60)))
    }

    // endregion

    // region ageing without a new packet

    @Test
    fun `the same fresh position turns old as the clock advances`() {
        val fix = ago(9 * 60)
        assertEquals(PositionState.Fresh(9.minutes), NodeFreshness.position(true, fix, fix, now, t))
        assertEquals(PositionState.Stale(10.minutes + 15.seconds), NodeFreshness.position(true, fix, fix, now + 75, t))
    }

    @Test
    fun `the same contact stops being recent as the clock advances`() {
        val heard = ago(14 * 60)
        assertEquals(ContactState.SeenRecently(14.minutes), NodeFreshness.contact(heard, now, t))
        assertEquals(ContactState.NotHeardSince(16.minutes), NodeFreshness.contact(heard, now + 120, t))
    }

    // endregion

    // region node adapter

    @Test
    fun `node adapter reads timestamp and time from the stored position`() {
        val node =
            Node(
                num = 1,
                user = user("!00000001", "ALPHA-1", "A1"),
                lastHeard = ago(30),
                position =
                Position.Builder()
                    .also { wb ->
                        wb.latitude_i = 488_566_000
                        wb.longitude_i = 23_522_000
                        wb.time = ago(30)
                        wb.timestamp = ago(20 * 60)
                    }
                    .build(),
            )

        assertEquals(PositionState.Stale(20.minutes), NodeFreshness.position(node, now, t))
        assertEquals(ContactState.SeenRecently(30.seconds), NodeFreshness.contact(node, now, t))
    }

    @Test
    fun `node adapter reports no position for zero coordinates`() {
        val node = Node(num = 2, user = user("!00000002", "BRAVO-2", "B2"))
        assertEquals(PositionState.NoPosition, NodeFreshness.position(node, now, t))
        assertEquals(ContactState.NeverHeard, NodeFreshness.contact(node, now, t))
    }

    // endregion
}
