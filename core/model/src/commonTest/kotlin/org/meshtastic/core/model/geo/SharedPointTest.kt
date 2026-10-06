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
import org.meshtastic.core.model.utf8Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedPointTest {

    // 14:30 UTC on 6 October 2026.
    private val now = 1_791_297_000L
    private val alpha1 = 0x0A1A0001

    private fun share(message: String, callsign: String = "ALPHA-1", sender: Int = alpha1) =
        SharedPoint.of(message, callsign, sender, now, TimeZone.UTC)

    @Test
    fun observedFactIsNamedByItsTimeAndDescribedByTheFact() {
        val point =
            assertNotNull(share("[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 48251 11932 · deux véhicules arrêtés"))
        assertEquals("FO 14:05 ALPHA-1", point.name)
        assertEquals("deux véhicules arrêtés", point.description)
        assertEquals(SharedPoint.ICON_OBSERVED_FACT, point.icon)
    }

    @Test
    fun observedFactOfTheDayBeforeSaysSo() {
        val point = assertNotNull(share("[CR] FAIT OBSERVÉ · 23:50 (veille) · Lieu MGRS 31U DQ 48251 11932 · fumée"))
        assertEquals("FO 23:50 (veille) ALPHA-1", point.name)
    }

    @Test
    fun observedFactInTheEarlierFormatHasAnUnknownTime() {
        val point = assertNotNull(share("[CR] FAIT OBSERVÉ · Lieu MGRS 31U DQ 48251 11932 · fumée"))
        assertEquals("FO --:-- ALPHA-1", point.name)
        assertEquals("heure d'observation inconnue · fumée", point.description)
    }

    @Test
    fun freshPositionIsNamedByItsFixTime() {
        val point =
            assertNotNull(share("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · UTM 31U 448251 5411932 · relevée 14:28"))
        assertEquals("POS 14:28 ALPHA-1", point.name)
        assertEquals("relevée 14:28", point.description)
        assertEquals(SharedPoint.ICON_POSITION, point.icon)
    }

    @Test
    fun oldPositionNeverPassesForACurrentOne() {
        val old =
            assertNotNull(
                share("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · POSITION ANCIENNE, relevée il y a 25 min (14:05)"),
            )
        assertEquals("POS ANC 14:05 ALPHA-1", old.name)
        assertTrue(old.description.startsWith("POSITION ANCIENNE"))

        val oldUnknown =
            assertNotNull(
                share(
                    "[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · " +
                        "POSITION ANCIENNE, heure de relevé inconnue, reçue il y a 2 h 05",
                ),
            )
        assertEquals("POS ANC --:-- ALPHA-1", oldUnknown.name)
        assertTrue(oldUnknown.description.startsWith("POSITION ANCIENNE, heure de relevé inconnue"))
    }

    @Test
    fun positionWithoutFixTimeSaysSo() {
        val unknown = assertNotNull(share("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · heure de relevé inconnue"))
        assertEquals("POS --:-- ALPHA-1", unknown.name)
        assertEquals("heure de relevé inconnue", unknown.description)
        val inconsistent = assertNotNull(share("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · heure de relevé incohérente"))
        assertEquals("POS --:-- ALPHA-1", inconsistent.name)
        assertEquals("heure de relevé incohérente", inconsistent.description)
    }

    @Test
    fun phonePositionIsFlaggedInTheDescription() {
        val point = assertNotNull(share("[POS] ALPHA-1 (téléphone) · MGRS 31U DQ 48251 11932 · relevée 14:28"))
        assertEquals("relevée 14:28 · position du téléphone", point.description)
    }

    @Test
    fun otherMessageCarriesItsSendingTime() {
        val point = assertNotNull(share("Regroupement 31U DQ 48251 11932"))
        assertEquals("PT ALPHA-1", point.name)
        assertEquals("envoyé 14:30 · Regroupement 31U DQ 48251 11932", point.description)
        assertEquals(SharedPoint.ICON_POINT, point.icon)
    }

    @Test
    fun messageWithoutCoordinateSharesNothing() {
        assertNull(share("[CR] Point de situation à 14:05"))
        assertNull(share("Appelez le 06 12 34 56 78"))
    }

    @Test
    fun pointIsTheCoordinateOfTheMessage() {
        val point = assertNotNull(share("[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 48251 11932 · fumée")).point
        val expected = (CoordinateParser.parse("31U DQ 48251 11932", CoordinateFormat.MGRS) as CoordinateInput.Valid)
        assertEquals(expected.point, point)
    }

    @Test
    fun validityIsTwentyFourHoursAndTheSenderAloneMayChangeIt() {
        val point = assertNotNull(share("[POS] ALPHA-1 · MGRS 31U DQ 48251 11932 · relevée 14:28"))
        assertEquals(now + 24 * 3_600, point.expireEpochSeconds)
        assertEquals(alpha1, point.lockedTo)
    }

    @Test
    fun nameAndDescriptionFitTheWaypointLimits() {
        val long = "é".repeat(80)
        val point =
            assertNotNull(
                share(
                    "[CR] FAIT OBSERVÉ · 23:50 (veille) · Lieu MGRS 31U DQ 48251 11932 · $long",
                    callsign = "CHARLIE-3-INDICATIF-TRÈS-LONG",
                ),
            )
        assertTrue(point.name.utf8Size() <= SharedPoint.NAME_MAX_BYTES, point.name)
        assertTrue(point.name.startsWith("FO 23:50 (veille) CHARLIE"), point.name)
        assertTrue(point.description.utf8Size() <= SharedPoint.DESCRIPTION_MAX_BYTES)
        assertTrue(point.description.endsWith("…"))
        assertTrue(point.description.startsWith("éé"))
    }

    @Test
    fun emojiIsNeverCutInHalf() {
        val point = assertNotNull(share("Regroupement 31U DQ 48251 11932 " + "🔥".repeat(40)))
        assertTrue(point.description.utf8Size() <= SharedPoint.DESCRIPTION_MAX_BYTES)
        assertTrue(point.description.removeSuffix("…").endsWith("🔥"))
    }

    @Test
    fun sameMessageSharedTwiceIsTheSameWaypoint() {
        val text = "[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 48251 11932 · fumée"
        val first = assertNotNull(share(text))
        val again = assertNotNull(SharedPoint.of(text, "ALPHA-1", alpha1, now + 600, TimeZone.UTC))
        assertEquals(first.id, again.id)
        assertNotEquals(0, first.id)
    }

    @Test
    fun differentMessagesOrSendersAreDifferentWaypoints() {
        val text = "[CR] FAIT OBSERVÉ · 14:05 · Lieu MGRS 31U DQ 48251 11932 · fumée"
        val id = assertNotNull(share(text)).id
        assertNotEquals(id, assertNotNull(share(text.replace("fumée", "flammes"))).id)
        assertNotEquals(id, assertNotNull(share(text, sender = 0x0B2B0002)).id)
    }
}
