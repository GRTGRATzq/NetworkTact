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
package org.meshtastic.feature.map.maplibre.component

import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.geo.MapPointCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MapPointStateTest {

    @Test
    fun `a long press shows the coordinate of the pressed point`() {
        val state = MapPointState()
        val pressed = Position(longitude = 23.3545, latitude = -45.6456)

        state.show(pressed)

        assertEquals("34G FQ 83473 42631", state.point?.formatted?.mgrs)
        assertEquals(MapPointCoordinate.of(-45.6456, 23.3545), state.point)
        assertEquals(pressed, state.pressed)
    }

    @Test
    fun `a point past the antimeridian reads as the same place`() {
        val state = MapPointState()
        state.show(Position(longitude = 362.0, latitude = 48.0))
        assertEquals(MapPointCoordinate.of(48.0, 2.0)?.formatted, state.point?.formatted)
    }

    @Test
    fun `a new press replaces the card and forgets the copy`() {
        val state = MapPointState()
        state.show(Position(longitude = 2.0, latitude = 48.0))
        state.copied = true

        state.show(Position(longitude = 3.0, latitude = 47.0))

        assertEquals(MapPointCoordinate.of(47.0, 3.0), state.point)
        assertFalse(state.copied)
    }

    @Test
    fun `closing the card removes the point and its marker`() {
        val state = MapPointState()
        state.show(Position(longitude = 2.0, latitude = 48.0))

        state.dismiss()

        assertNull(state.point)
        assertNull(state.pressed)
    }
}
