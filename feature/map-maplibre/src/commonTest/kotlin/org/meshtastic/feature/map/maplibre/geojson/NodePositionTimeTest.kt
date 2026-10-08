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
package org.meshtastic.feature.map.maplibre.geojson

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.proto.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The chip and the tap card read a position with the command post view's own rule; these are its cases on the map. */
class NodePositionTimeTest {

    // 2026-10-08 12:00:00 UTC.
    private val now = 1_791_460_800L
    private val minute = 60L

    private fun node(num: Int = 1, fixSeconds: Long = 0L, timeSeconds: Long = 0L) = Node(
        num = num,
        position =
        Position.Builder()
            .also {
                it.latitude_i = 434_500_000
                it.longitude_i = 64_600_000
                it.timestamp = fixSeconds.toInt()
                it.time = timeSeconds.toInt()
            }
            .build(),
    )

    @Test
    fun freshPositionHasNoTagAndShowsItsTimeOfTheDay() {
        val time = node(fixSeconds = now - 2 * minute).positionTime(now, TimeZone.UTC)
        assertIs<PositionState.Fresh>(time.state)
        assertNull(time.state.chipAlert)
        assertEquals(now - 2 * minute, time.fixEpochSeconds)
        assertTrue(time.sameDay)
    }

    @Test
    fun positionPastTenMinutesIsTaggedOld() {
        val time = node(fixSeconds = now - 11 * minute).positionTime(now, TimeZone.UTC)
        assertIs<PositionState.Stale>(time.state)
        assertEquals(ChipAlert.OLD, time.state.chipAlert)
    }

    @Test
    fun positionAtTenMinutesIsStillFresh() {
        assertNull(node(fixSeconds = now - 10 * minute).positionTime(now, TimeZone.UTC).state.chipAlert)
    }

    @Test
    fun positionWithoutAnyTimeHasUnknownTimeAndNoTag() {
        val time = node().positionTime(now, TimeZone.UTC)
        assertEquals(PositionState.NoFixTime, time.state)
        assertNull(time.state.chipAlert)
        assertNull(time.fixEpochSeconds)
        assertFalse(time.sameDay)
    }

    @Test
    fun positionReceivedLongAgoWithoutFixTimeIsTaggedOldWithUnknownTime() {
        val time = node(timeSeconds = now - 120 * minute).positionTime(now, TimeZone.UTC)
        assertIs<PositionState.StaleAtLeast>(time.state)
        assertEquals(ChipAlert.OLD, time.state.chipAlert)
        assertNull(time.fixEpochSeconds)
    }

    @Test
    fun positionReceivedRecentlyWithoutFixTimeIsNeverFreshButNotTagged() {
        val time = node(timeSeconds = now - 2 * minute).positionTime(now, TimeZone.UTC)
        assertIs<PositionState.ReceivedFixTimeUnknown>(time.state)
        assertNull(time.state.chipAlert)
        assertNull(time.fixEpochSeconds)
    }

    @Test
    fun positionFromAnotherDayShowsItsDateAndIsTaggedOld() {
        val time = node(fixSeconds = now - 13 * 60 * minute).positionTime(now, TimeZone.UTC)
        assertFalse(time.sameDay)
        assertEquals(ChipAlert.OLD, time.state.chipAlert)
    }

    @Test
    fun dayIsReadInThePhoneTimeZone() {
        // 23:30 UTC the day before is 01:30 in Paris on the same day as noon UTC.
        val lateEvening = now - (12 * 60 + 30) * minute
        assertFalse(node(fixSeconds = lateEvening).positionTime(now, TimeZone.UTC).sameDay)
        assertTrue(node(fixSeconds = lateEvening).positionTime(now, TimeZone.of("Europe/Paris")).sameDay)
    }

    @Test
    fun fixTimeAheadOfThePhoneClockIsTaggedInconsistent() {
        val time = node(fixSeconds = now + 5 * minute).positionTime(now, TimeZone.UTC)
        assertIs<PositionState.InconsistentTimestamp>(time.state)
        assertEquals(ChipAlert.INCONSISTENT, time.state.chipAlert)
    }

    @Test
    fun alertsKeepOnlyTaggedNodes() {
        val alerts =
            positionAlerts(
                listOf(node(1, fixSeconds = now - minute), node(2, fixSeconds = now - 30 * minute), node(3)),
                now,
            )
        assertEquals(mapOf(2 to ChipAlert.OLD), alerts)
    }

    @Test
    fun taggedChipLooksDifferentFromPlainChip() {
        val plain = node(1).toNodeChip()
        val old = node(1).toNodeChip(ChipAlert.OLD)
        assertNotEquals(plain.featureValue(), old.featureValue())
        assertNotEquals(old.featureValue(), node(1).toNodeChip(ChipAlert.INCONSISTENT).featureValue())
    }
}
