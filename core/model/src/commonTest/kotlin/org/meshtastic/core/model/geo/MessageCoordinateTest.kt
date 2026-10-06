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

class MessageCoordinateTest {

    private fun assertNear(expected: LatLon, actual: LatLon) {
        assertTrue(abs(expected.latitude - actual.latitude) < TOLERANCE, "latitude ${actual.latitude}")
        assertTrue(abs(expected.longitude - actual.longitude) < TOLERANCE, "longitude ${actual.longitude}")
    }

    private fun mgrsPoint(text: String) =
        (CoordinateParser.parse(text, CoordinateFormat.MGRS) as CoordinateInput.Valid).point

    @Test
    fun positionMessageGivesItsMgrs() {
        val found =
            assertNotNull(
                MessageCoordinate.find(
                    "[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · UTM 31U 448251 5411932 · " +
                        "DMS 48°51'24\"N 002°21'03\"E · relevée 14:32",
                ),
            )
        assertEquals(CoordinateFormat.MGRS, found.format)
        assertEquals("31U DQ 48251 11932", found.text)
        assertNear(mgrsPoint("31U DQ 48251 11932"), found.point)
    }

    @Test
    fun observedFactInBothFormatsGivesItsPlace() {
        val new = "[CR] FAIT OBSERVÉ · 14:05 · Lieu DMS 48°51'24\"N 002°21'03\"E = MGRS 31U DQ 52382 11725 · fumée"
        val old = "[CR] FAIT OBSERVÉ · Lieu UTM 31U 448251 5411932 · pont coupé"
        assertEquals("31U DQ 52382 11725", MessageCoordinate.find(new)?.text)
        val fromOld = assertNotNull(MessageCoordinate.find(old))
        assertEquals(CoordinateFormat.UTM, fromOld.format)
        assertNear(mgrsPoint("31U DQ 48251 11932"), fromOld.point)
    }

    @Test
    fun pastedCoordinateInFreeTextIsFound() {
        assertEquals("31UDQ4825111932", MessageCoordinate.find("rdv 31udq4825111932 à 15h")?.text)
        val dms = assertNotNull(MessageCoordinate.find("Regroupement 48 51 24 N 2 21 3 E derrière l'église"))
        assertEquals(CoordinateFormat.DMS, dms.format)
        assertNear(LatLon(48.856_667, 2.350_833), dms.point)
        assertEquals(CoordinateFormat.DMS, MessageCoordinate.find("point 48°51'24\"N 2°21'03\"O")?.format)
    }

    @Test
    fun phoneNumberIsNotAPosition() {
        assertNull(MessageCoordinate.find("Appelez le 06 12 34 56 78"))
        assertNull(MessageCoordinate.find("+33 6 12 34 56 78"))
        assertNull(MessageCoordinate.find("0612345678"))
    }

    @Test
    fun timesAndDatesAreNotPositions() {
        assertNull(MessageCoordinate.find("[CR] Point de situation à 14:05"))
        assertNull(MessageCoordinate.find("Départ 14h05, retour 18 30"))
        assertNull(MessageCoordinate.find("Le 06/10/2026 à 14:05:30"))
        assertNull(MessageCoordinate.find("2026-10-06T14:05:00Z"))
        assertNull(MessageCoordinate.find("6 oct. 2026"))
    }

    @Test
    fun referencesAndLoneNumbersAreNotPositions() {
        assertNull(MessageCoordinate.find("Dossier 4825111932"))
        assertNull(MessageCoordinate.find("Réf. 2026 4825 1193"))
        assertNull(MessageCoordinate.find("Lot 12 A 448251 5411932"))
        assertNull(MessageCoordinate.find("Effectif 12"))
        assertNull(MessageCoordinate.find("48"))
        assertNull(MessageCoordinate.find(""))
    }

    @Test
    fun incompleteOrImpossibleGridsAreNotPositions() {
        // Zone 61 and band I do not exist; DQ does not belong to zone 32; four digits per axis is not 1 m.
        assertNull(MessageCoordinate.find("61U DQ 48251 11932"))
        assertNull(MessageCoordinate.find("31I DQ 48251 11932"))
        assertNull(MessageCoordinate.find("32U DQ 48251 11932"))
        assertNull(MessageCoordinate.find("31U DQ 4825 1193"))
        assertNull(MessageCoordinate.find("31U DQ 48251"))
        // UTM with an easting out of range, or without its band.
        assertNull(MessageCoordinate.find("31U 048251 5411932"))
        assertNull(MessageCoordinate.find("31 448251 5411932"))
    }

    @Test
    fun dmsNeedsBothHemispheres() {
        assertNull(MessageCoordinate.find("48 51 24 2 21 3"))
        assertNull(MessageCoordinate.find("48°51'24\" 002°21'03\""))
        assertNull(MessageCoordinate.find("48°51'24\"N 002°21'03\""))
        assertNull(MessageCoordinate.find("48°51'24\"N 002°21'03\"EST"))
        assertNull(MessageCoordinate.find("48°75'24\"N 002°21'03\"E"))
    }

    private companion object {
        const val TOLERANCE = 0.000_1
    }
}
