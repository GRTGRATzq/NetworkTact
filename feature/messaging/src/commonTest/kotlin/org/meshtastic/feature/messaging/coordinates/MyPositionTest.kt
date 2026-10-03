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
package org.meshtastic.feature.messaging.coordinates

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.Node
import org.meshtastic.feature.messaging.priority.MessagePriority
import org.meshtastic.proto.Position
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MyPositionTest {

    /** 2026-10-03 14:32:00 UTC. */
    private val now = 1_791_037_920L

    private val user =
        User.Builder()
            .also { wb ->
                wb.id = "!a1a1a1a1"
                wb.long_name = "ALPHA-1 [Alpha]"
                wb.short_name = "A1"
            }
            .build()

    private fun radio(timestamp: Int = 0, withPosition: Boolean = true) = Node(
        num = 1,
        user = user,
        position =
        if (withPosition) {
            Position.Builder()
                .also { wb ->
                    wb.latitude_i = 488_566_670
                    wb.longitude_i = 23_508_330
                    wb.timestamp = timestamp
                }
                .build()
        } else {
            Position.Builder().build()
        },
    )

    private fun build(node: Node?, phone: PhoneFix? = null) = myPositionMessage(node, phone, now, TimeZone.UTC)

    @Test
    fun radio_position_with_the_team_suffix_removed_from_the_name() {
        val result = assertIs<MyPositionResult.Filled>(build(radio(timestamp = (now - 120).toInt())))
        assertEquals(
            "[POS] ALPHA-1 · MGRS 31U DQ 52382 11725 · UTM 31U 452382 5411725 · DMS 48°51'24\"N 002°21'03\"E · " +
                "relevée 14:30",
            result.text,
        )
        assertFalse(result.fromPhone)
        assertFalse(result.shortened)
        assertEquals(MessagePriority.INFO, MessagePriority.of(result.text))
    }

    @Test
    fun old_radio_position_is_flagged() {
        val result = assertIs<MyPositionResult.Filled>(build(radio(timestamp = (now - 25 * 60).toInt())))
        assertTrue(result.text.endsWith(" · POSITION ANCIENNE, relevée il y a 25 min (14:07)"))
    }

    @Test
    fun radio_position_without_fix_time_is_flagged() {
        val result = assertIs<MyPositionResult.Filled>(build(radio(timestamp = 0)))
        assertTrue(result.text.endsWith(" · heure de relevé inconnue"))
    }

    @Test
    fun phone_is_used_only_when_the_radio_has_no_position() {
        val phone = PhoneFix(latitude = -33.857, longitude = 151.215, fixEpochSeconds = now - 60)
        val fromPhone = assertIs<MyPositionResult.Filled>(build(radio(withPosition = false), phone))
        assertTrue(fromPhone.fromPhone)
        assertTrue(fromPhone.text.startsWith("[POS] ALPHA-1 (téléphone) · MGRS 56H LH "))
        assertTrue(fromPhone.text.endsWith(" · relevée 14:31"))

        val fromRadio = assertIs<MyPositionResult.Filled>(build(radio(timestamp = (now - 60).toInt()), phone))
        assertFalse(fromRadio.fromPhone)
        assertTrue(fromRadio.text.contains("31U DQ"))
    }

    @Test
    fun nothing_without_any_position() {
        assertEquals(MyPositionResult.NoPosition, build(radio(withPosition = false)))
        assertEquals(MyPositionResult.NoPosition, build(null, PhoneFix(48.0, 2.0, now)))
    }
}
