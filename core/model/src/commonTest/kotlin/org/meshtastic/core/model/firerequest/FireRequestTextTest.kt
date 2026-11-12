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

class FireRequestTextTest {

    private fun lines(request: FireRequest) = FireRequestText.write(request).map { it.text }

    private fun box(n: Int, request: FireRequest) =
        FireRequestText.write(request).filter { it.box == n }.joinToString("\n") { it.text }

    @Test
    fun specification_example_line_by_line() {
        val expected = FireRequestExamples.specificationParts.flatMap { it.lines().drop(1) }
        assertEquals(expected, lines(FireRequestExamples.specification))
    }

    @Test
    fun empty_request_has_the_eleven_boxes_and_no_header() {
        assertEquals((1..11).map { "$it non rempli" }, lines(FireRequest()))
    }

    @Test
    fun boxes_are_numbered_in_order() {
        val boxes = FireRequestText.write(FireRequestExamples.longest).map { it.box }
        assertEquals(boxes.sorted(), boxes)
        assertEquals((0..11).toSet(), boxes.toSet())
    }

    @Test
    fun header_leaves_out_empty_elements() {
        assertEquals("Origine OA", lines(FireRequest(origin = "OA")).first())
        assertEquals("GDH demande 112150Z NOV 26", lines(FireRequest(requestGdh = "112150Z NOV 26")).first())
    }

    @Test
    fun box_2_letters_come_first() {
        val box = FireType(lines = setOf(ImpactLine.G, ImpactLine.A), firstRound = true, setUp = "2", prepare = true)
        assertEquals("2 A G Emblée · Mettre en place 2 · Préparer", box(2, FireRequest(fireType = box)))
        assertEquals("2 A G", box(2, FireRequest(fireType = FireType(lines = setOf(ImpactLine.A, ImpactLine.G)))))
        assertEquals("2 Renouveler", box(2, FireRequest(fireType = FireType(renew = ""))))
    }

    @Test
    fun box_3_mix_and_illumination() {
        val effect = Effect(neutralisation = true, mixFirst = "4A", mixSecond = "2")
        assertEquals("3 Neutralisation · Panaché 4A et 2", box(3, FireRequest(effect = effect)))
        assertEquals("3 Éclairement 2", box(3, FireRequest(effect = Effect(illumination = "2"))))
        assertEquals("3 Panaché 1", box(3, FireRequest(effect = Effect(mixFirst = "1"))))
    }

    // Box 4 to 11: table D26 of docs/PLAN-DEMANDE-DE-TIR.md, validated box by box.

    @Test
    fun box_4() {
        assertEquals(
            "4 Nb de cps 2 / 6 · Durée 1 / 3 min · Intervalle entre les coups 10 / 20 s",
            box(4, FireRequest(observation = Observation("2", "6", "1", "3", "10", "20"))),
        )
        assertEquals(
            "4 Nb de cps / 6 · Intervalle entre les coups 10 / s",
            box(4, FireRequest(observation = Observation(shotsSecond = "6", intervalFirst = "10"))),
        )
        assertEquals("4 non rempli", box(4, FireRequest()))
    }

    @Test
    fun box_5() {
        assertEquals(
            "5 Lanceurs 2 · Trajectoire 1 / 3 · Mode 2 et 3 · Court retard 30 ms · Fusant 10 m",
            box(
                5,
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
                ),
            ),
        )
        assertEquals(
            "5 Lanceurs 1 · Trajectoire 1 · Mode 1",
            box(5, FireRequest(charge = Charge(launcher = "1", trajectoryFirst = "1", modeFirst = "1"))),
        )
        assertEquals(
            "5 Lanceurs 4 · Mode 4 · Retard 20 m",
            box(5, FireRequest(charge = Charge(launcher = "4", modeFirst = "4", delayM = "20"))),
        )
    }

    @Test
    fun box_6() {
        assertEquals(
            "6 Pick-up en mouvement · Personnel abrité · Artillerie pièces",
            box(
                6,
                FireRequest(
                    targets =
                    Targets(
                        pickupMotion = Motion.MOVING,
                        personnelCover = Cover.SHELTERED,
                        artilleryPart = ArtilleryPart.PIECES,
                    ),
                ),
            ),
        )
        assertEquals("6 Chars · Bâtiment", box(6, FireRequest(targets = Targets(tanks = true, building = true))))
        assertEquals(
            "6 Blindés légers · Pont",
            box(6, FireRequest(targets = Targets(lightArmour = true, other = "Pont"))),
        )
        assertEquals(
            "6 Chars à l'arrêt · Blindés légers en mouvement · Pick-up",
            box(
                6,
                FireRequest(
                    targets = Targets(tanksMotion = Motion.STOPPED, lightArmourMotion = Motion.MOVING, pickup = true),
                ),
            ),
        )
    }

    @Test
    fun box_7_has_one_line_per_line_of_the_sheet() {
        val location =
            Location(
                a = PolarLine("1600", "2500", "20"),
                d = RectangularLine("34550", "88800", "640"),
                reference =
                ReferenceLine(
                    first = "R1",
                    second = "R2",
                    eastWest = Shift.EAST,
                    eastWestM = "200",
                    nearFar = Shift.NEARER,
                    nearFarM = "100",
                ),
                g = LruLine("35", "T", "LL", "33000", "90300", "610"),
                tle = "2",
            )
        assertEquals(
            """
            7 A G 1600 · D 2500 m · S 20 m
            7 D X 34550 · Y 88800 · Z 640 m
            7 Rep R1 R2 · Plus à l'est 200 m · Plus près 100 m
            7 G 35T LL 33000 - 90300 · Z 610 m · TLE 2
            """
                .trimIndent(),
            box(7, FireRequest(location = location)),
        )
        assertEquals("7 TLE 3", box(7, FireRequest(location = Location(tle = "3"))))
        assertEquals("7 non rempli", box(7, FireRequest()))
    }

    @Test
    fun box_8() {
        assertEquals("8 Convergent", box(8, FireRequest(shape = Shape(kind = ShapeKind.CONVERGENT))))
        assertEquals(
            "8 Linéaire · Point central P1 · LG 200 · Gisement 1600 · Front 400 m · Profondeur 100 m · Ouvert",
            box(
                8,
                FireRequest(
                    shape =
                    Shape(
                        kind = ShapeKind.LINEAR,
                        centralPoint = "P1",
                        side = Side.LG,
                        sideValue = "200",
                        bearing = "1600",
                        front = "400",
                        depth = "100",
                        openness = Openness.OPEN,
                    ),
                ),
            ),
        )
        assertEquals(
            "8 Circulaire · Centre P2 · Rayon 100 m",
            box(8, FireRequest(shape = Shape(kind = ShapeKind.CIRCULAR, centre = "P2", radius = "100"))),
        )
    }

    @Test
    fun box_10() {
        assertEquals(
            "10 Distance 1200 m · Gisement N · Niveau de protection 3",
            box(10, FireRequest(friends = Friends("1200", "N", "3"))),
        )
    }

    @Test
    fun box_11_takes_several_choices() {
        assertEquals(
            "11 Aussitôt prêt limité NLT 14H30",
            box(11, FireRequest(command = Command(whenReadyNlt = "14H30"))),
        )
        assertEquals(
            "11 HSO limité début 14H30 fin 14H45",
            box(11, FireRequest(command = Command(limitedStart = "14H30", limitedEnd = "14H45"))),
        )
        assertEquals(
            "11 À mon commandement · Synchro S1 · Heure sur objectif 14H30 · Heure de tir 14H25 · Au commandement de OA",
            box(
                11,
                FireRequest(
                    command =
                    Command(
                        atMyCommand = true,
                        synchro = "S1",
                        timeOnTarget = "14H30",
                        fireTime = "14H25",
                        atCommandOf = "OA",
                    ),
                ),
            ),
        )
    }

    @Test
    fun free_text_loses_separators_and_line_breaks() {
        assertEquals("6 Pont détruit", box(6, FireRequest(targets = Targets(other = " Pont ·\ndétruit "))))
    }
}
