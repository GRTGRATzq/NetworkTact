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
package org.meshtastic.core.model.firerequest

import kotlin.test.Test
import kotlin.test.assertEquals

/** The « Affichage » column of table D26 and the mock-up's reconstructed sheet. */
class FireRequestDisplayTest {

    private val example = FireRequestExamples.specification
    private val m = FireRequestDisplay.MILS

    private fun lines(box: Int, request: FireRequest) = FireRequestDisplay.lines(request, box)

    @Test
    fun specification_example_as_on_the_mock_up() {
        assertEquals(listOf("AB1234"), lines(1, example))
        assertEquals(listOf("G Emblée · Préparer"), lines(2, example))
        assertEquals(listOf("Neutralisation", "Panaché 1 Explo et 2 Fumigène"), lines(3, example))
        assertEquals(emptyList(), lines(4, example))
        assertEquals(
            listOf("Lanceurs 2 : Mo 120", "Trajectoire 1 : Plongeante", "Mode 1 : Percutant instantané"),
            lines(5, example),
        )
        assertEquals(listOf("Personnel à découvert"), lines(6, example))
        assertEquals(listOf("Circulaire · Rayon 100 m"), lines(8, example))
        assertEquals(
            listOf("Grille", "Gisement d'observ. 4800 $m", "Observateur X 34550 · Y 88800 · Z 640"),
            lines(9, example),
        )
        assertEquals(
            listOf("Distance 800 m · Gisement 1600 $m", "Niveau de protection 1 : à découvert"),
            lines(10, example),
        )
        assertEquals(listOf("Aussitôt prêt"), lines(11, example))
        assertEquals(
            listOf(
                LocationEntry("L R U", "G", listOf("35T LL 33000 - 90300", "Z 610 m")),
                LocationEntry("TLE", null, listOf("TLE 2")),
            ),
            FireRequestDisplay.location(example.location),
        )
    }

    @Test
    fun box_4() {
        val request = FireRequest(observation = Observation(shotsSecond = "6", intervalFirst = "10"))
        assertEquals(listOf("Nb de cps / 6", "Intervalle entre les coups 10 / s"), lines(4, request))
    }

    @Test
    fun box_5_modes_take_their_delay() {
        val request =
            FireRequest(
                charge =
                Charge(
                    launcher = "2",
                    trajectoryFirst = "1",
                    trajectorySecond = "3",
                    modeFirst = "2",
                    modeSecond = "3",
                    shortDelayMs = "30",
                    airburstM = "10",
                ),
            )
        assertEquals(
            listOf(
                "Lanceurs 2 : Mo 120",
                "Trajectoire 1 : Plongeante / 3 : Nominale",
                "Mode 2 : Percutant court retard 30 ms et 3 : Fusant 10 m",
            ),
            lines(5, request),
        )
        assertEquals(
            listOf("Lanceurs 4 : LRU", "Mode 4 : Retard 20 m"),
            lines(5, FireRequest(charge = Charge(launcher = "4", modeFirst = "4", delayM = "20"))),
        )
    }

    @Test
    fun box_6_box_8_and_box_10() {
        assertEquals(
            listOf("Blindés légers", "Pont"),
            lines(6, FireRequest(targets = Targets(lightArmour = true, other = "Pont"))),
        )
        val linear =
            Shape(
                kind = ShapeKind.LINEAR,
                centralPoint = "P1",
                side = Side.LG,
                sideValue = "200",
                bearing = "1600",
                front = "400",
                depth = "100",
                openness = Openness.OPEN,
            )
        assertEquals(
            listOf("Linéaire · Point central P1 · LG 200 · Gisement 1600 $m · Front 400 m · Profondeur 100 m · Ouvert"),
            lines(8, FireRequest(shape = linear)),
        )
        assertEquals(
            listOf("Distance 1200 m · Gisement N", "Niveau de protection 3 : sous blindage"),
            lines(10, FireRequest(friends = Friends("1200", "N", "3"))),
        )
    }

    @Test
    fun box_3_mix_with_illumination_and_illumination_alone() {
        assertEquals(
            listOf("Éclairement 2 : IR", "Panaché 4A ECL VISIB et 2 Fumigène"),
            lines(3, FireRequest(effect = Effect(illumination = "2", mixFirst = "4A", mixSecond = "2"))),
        )
    }

    @Test
    fun box_7_polar_lines_mark_the_bearing() {
        val entries = FireRequestDisplay.location(Location(a = PolarLine("1600", "2500", "20")))
        assertEquals(listOf(LocationEntry("POLAIRES", "A", listOf("G 1600 $m · D 2500 m · S 20 m"))), entries)
    }
}
