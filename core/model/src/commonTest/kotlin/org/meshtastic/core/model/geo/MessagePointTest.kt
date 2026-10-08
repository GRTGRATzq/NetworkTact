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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessagePointTest {

    @Test
    fun observedFactGivesItsObservationTime() {
        val point = MessagePoint.of("[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 48251 11932 · deux véhicules")
        assertEquals(MessagePoint.Kind.OBSERVED_FACT, point.kind)
        assertEquals("14:05", point.clock)
        assertFalse(point.old)
    }

    @Test
    fun observedFactOfTheDayBeforeKeepsTheMention() {
        val point = MessagePoint.of("[CR] FAIT OBSERVÉ · 23:50 (veille) · Lieu MGRS 31U DQ 48251 11932 · fumée")
        assertEquals("23:50 (veille)", point.clock)
    }

    @Test
    fun observedFactInTheEarlierFormatHasNoTime() {
        assertNull(MessagePoint.of("[CR] FAIT OBSERVÉ · Lieu MGRS 31U DQ 48251 11932 · fumée").clock)
    }

    @Test
    fun freshPositionGivesItsFixTime() {
        val point = MessagePoint.of("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · relevée 14:28")
        assertEquals(MessagePoint.Kind.POSITION, point.kind)
        assertEquals("14:28", point.clock)
        assertFalse(point.old)
        assertFalse(point.fromPhone)
    }

    @Test
    fun oldPositionIsMarkedOld() {
        val point =
            MessagePoint.of(
                "[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · POSITION ANCIENNE, relevée il y a 25 min (14:05)",
            )
        assertTrue(point.old)
        assertEquals("14:05", point.clock)
    }

    @Test
    fun oldPositionWithoutFixTimeIsOldWithNoTime() {
        val point =
            MessagePoint.of(
                "[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · POSITION ANCIENNE, heure de relevé inconnue, reçue il y a 2 h 05",
            )
        assertTrue(point.old)
        assertNull(point.clock)
    }

    @Test
    fun positionWithoutFixTimeHasNoTime() {
        val point = MessagePoint.of("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · heure de relevé inconnue")
        assertNull(point.clock)
        assertFalse(point.old)
        assertFalse(point.inconsistent)
    }

    @Test
    fun inconsistentFixTimeIsFlaggedAndHasNoTime() {
        val point = MessagePoint.of("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · heure de relevé incohérente")
        assertTrue(point.inconsistent)
        assertNull(point.clock)
    }

    @Test
    fun phonePositionIsFlagged() {
        assertTrue(MessagePoint.of("[POS] ALPHA-1 (téléphone) · MGRS 31U DQ 48251 11932 · relevée 14:28").fromPhone)
    }

    @Test
    fun otherMessageGivesNoTime() {
        val point = MessagePoint.of("Regroupement 31U DQ 48251 11932 à 14:30")
        assertEquals(MessagePoint.Kind.POINT, point.kind)
        assertNull(point.clock)
        assertFalse(point.old)
    }
}
