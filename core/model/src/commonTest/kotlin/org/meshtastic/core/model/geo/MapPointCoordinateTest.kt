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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapPointCoordinateTest {

    @Test
    fun touchedPointIsGivenToOneMetreInMgrs() {
        val point = assertNotNull(MapPointCoordinate.of(-45.6456, 23.3545))
        assertEquals("34G FQ 83473 42631", point.formatted.mgrs)
        assertEquals(Mgrs.fromLatLon(LatLon(-45.6456, 23.3545))?.format(), point.formatted.mgrs)
        assertEquals(Utm.fromLatLon(LatLon(-45.6456, 23.3545))?.format(), point.formatted.utm)
        assertEquals(Dms.format(LatLon(-45.6456, 23.3545)), point.formatted.dms)
    }

    @Test
    fun messageTextGivesTheThreeFormatsMgrsFirst() {
        val point = assertNotNull(MapPointCoordinate.of(-45.6456, 23.3545))
        val f = point.formatted
        assertEquals("Point carte · MGRS ${f.mgrs} · UTM ${f.utm} · DMS ${f.dms}", point.messageText)
    }

    @Test
    fun messageTextIsReadBackAsTheSamePoint() {
        val point = assertNotNull(MapPointCoordinate.of(48.856667, 2.350833))
        val found = assertNotNull(MessageCoordinate.find(point.messageText))
        assertEquals(CoordinateFormat.MGRS, found.format)
        // MGRS to 1 m is truncated, so the point read back lies within about 1.5 m of the touched one.
        assertTrue(abs(found.point.latitude - 48.856667) < 0.00002)
        assertTrue(abs(found.point.longitude - 2.350833) < 0.00003)
    }

    @Test
    fun messageTextFitsInOneMessage() {
        val point = assertNotNull(MapPointCoordinate.of(-12.765, -33.8765))
        assertTrue(point.messageText.encodeToByteArray().size <= PositionMessage.MAX_MESSAGE_BYTES)
    }

    @Test
    fun polarPointGivesOnlyDms() {
        val point = assertNotNull(MapPointCoordinate.of(85.0, 10.0))
        assertNull(point.formatted.mgrs)
        assertEquals("Point carte · DMS ${point.formatted.dms}", point.messageText)
    }

    @Test
    fun longitudePastTheAntimeridianIsBroughtBack() {
        assertEquals(-170.0, MapPointCoordinate.normalizeLongitude(190.0), 1e-9)
        assertEquals(170.0, MapPointCoordinate.normalizeLongitude(-190.0), 1e-9)
        assertEquals(2.35, MapPointCoordinate.normalizeLongitude(362.35), 1e-9)
        assertEquals(-180.0, MapPointCoordinate.normalizeLongitude(180.0), 1e-9)
        assertEquals(
            MapPointCoordinate.of(48.0, 2.0)?.formatted?.mgrs,
            MapPointCoordinate.of(48.0, 362.0)?.formatted?.mgrs,
        )
    }

    @Test
    fun pointOffTheEarthIsRefused() {
        assertNull(MapPointCoordinate.of(91.0, 0.0))
        assertNull(MapPointCoordinate.of(Double.NaN, 0.0))
        assertNull(MapPointCoordinate.of(0.0, Double.POSITIVE_INFINITY))
    }
}
