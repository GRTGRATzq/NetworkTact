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
package org.meshtastic.core.model.geo

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.freshness.NodeFreshness
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.utf8Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PositionMessageTest {

    /** 2026-10-03 14:32:00 UTC. */
    private val now = 1_791_037_920L
    private val paris = LatLon(48.856667, 2.350833)

    private fun build(
        state: PositionState,
        name: String = "ALPHA-1",
        point: LatLon = paris,
        source: PositionSource = PositionSource.RADIO,
    ) = PositionMessage.build(name, point, source, state, now, TimeZone.UTC)

    @Test
    fun fresh_position_gives_all_three_formats_and_the_fix_time() {
        assertEquals(
            "[POS] ALPHA-1 · MGRS 31U DQ 52382 11725 · UTM 31U 452382 5411725 · DMS 48°51'24\"N 002°21'03\"E · " +
                "relevée 14:30",
            build(PositionState.Fresh(2.minutes)),
        )
    }

    @Test
    fun stale_position_says_so_with_its_age_and_clock_time() {
        assertTrue(
            build(PositionState.Stale(25.minutes)).endsWith(" · POSITION ANCIENNE, relevée il y a 25 min (14:07)"),
        )
    }

    @Test
    fun position_without_fix_time_never_reads_as_current() {
        assertTrue(build(PositionState.NoFixTime).endsWith(" · heure de relevé inconnue"))
        assertTrue(build(PositionState.ReceivedFixTimeUnknown(1.minutes)).endsWith(" · heure de relevé inconnue"))
        assertTrue(
            build(PositionState.StaleAtLeast(2.hours + 5.minutes))
                .endsWith(" · POSITION ANCIENNE, heure de relevé inconnue, reçue il y a 2 h 05"),
        )
        assertTrue(build(PositionState.InconsistentTimestamp(1.hours)).endsWith(" · heure de relevé incohérente"))
    }

    @Test
    fun same_threshold_as_the_command_post_view() {
        val fresh = NodeFreshness.position(true, (now - 10 * 60).toInt(), 0, now)
        val stale = NodeFreshness.position(true, (now - 10 * 60 - 1).toInt(), 0, now)
        assertTrue(build(fresh).endsWith("relevée 14:22"))
        assertTrue(build(stale).contains("POSITION ANCIENNE"))
    }

    @Test
    fun phone_position_is_labelled() {
        assertTrue(
            build(PositionState.Fresh(0.seconds), source = PositionSource.PHONE)
                .startsWith("[POS] ALPHA-1 (téléphone) · "),
        )
    }

    @Test
    fun ages() {
        assertEquals("11 min", PositionMessage.age(11.minutes + 59.seconds))
        assertEquals("1 h 00", PositionMessage.age(60.minutes))
        assertEquals("23 h 59", PositionMessage.age(24.hours - 1.minutes))
        assertEquals("3 j", PositionMessage.age(80.hours))
    }

    @Test
    fun drops_dms_then_utm_to_fit_200_bytes_and_keeps_mgrs_and_time() {
        val longName = "BRAVO-2 ÉQUIPE DE RECONNAISSANCE NORD1" // 39 bytes, the long name maximum
        assertEquals(39, longName.utf8Size())
        val stale = PositionState.StaleAtLeast(2.hours + 5.minutes)
        val message = build(stale, name = longName, source = PositionSource.PHONE)
        assertTrue(message.utf8Size() <= PositionMessage.MAX_MESSAGE_BYTES, "${message.utf8Size()} bytes")
        assertTrue(message.contains(" · MGRS 31U DQ 52382 11725 · "))
        assertTrue(message.contains("POSITION ANCIENNE"))
        assertTrue(!message.contains("DMS "))
    }

    @Test
    fun polar_position_only_has_dms() {
        assertEquals(
            "[POS] PC-0 · DMS 85°00'00\"N 010°00'00\"E · relevée 14:32",
            build(PositionState.Fresh(0.seconds), name = "PC-0", point = LatLon(85.0, 10.0)),
        )
    }

    @Test
    fun the_example_message_fits() {
        assertTrue(build(PositionState.Fresh(0.seconds)).utf8Size() <= PositionMessage.MAX_MESSAGE_BYTES)
    }
}
