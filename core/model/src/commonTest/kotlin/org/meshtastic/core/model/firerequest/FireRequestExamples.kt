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

/** Requests shared by the fire request tests. */
object FireRequestExamples {

    /** The validated example of `docs/SPEC-DEMANDE-DE-TIR.md` § 4. */
    val specification =
        FireRequest(
            origin = "OA",
            recipient = "CDS TIR",
            requestGdh = "112250 loc NOV 26",
            number = "AB1234",
            fireType = FireType(lines = setOf(ImpactLine.G), firstRound = true, prepare = true),
            effect = Effect(neutralisation = true, mixFirst = "1", mixSecond = "2"),
            charge = Charge(launcher = "2", trajectoryFirst = "1", modeFirst = "1"),
            targets = Targets(personnel = true, personnelCover = Cover.OPEN),
            location = Location(g = LruLine("35", "T", "LL", "33000", "90300", "610"), tle = "2"),
            shape = Shape(kind = ShapeKind.CIRCULAR, radius = "100"),
            adjustment =
            Adjustment(
                method = AdjustMethod.GRID,
                observationBearing = "4800",
                observerX = "34550",
                observerY = "88800",
                observerZ = "640",
            ),
            friends = Friends(distance = "800", bearing = "1600", protection = "1"),
            command = Command(whenReady = true),
        )

    /** The three parts of the validated example, as sent. */
    val specificationParts =
        listOf(
            """
            [DT AB1234 1/3]
            Origine OA · Destinataire CDS TIR · GDH demande 112250 loc NOV 26
            1 AB1234
            2 G Emblée · Préparer
            3 Neutralisation · Panaché 1 et 2
            4 non rempli
            """
                .trimIndent(),
            """
            [DT AB1234 2/3]
            5 Lanceurs 2 · Trajectoire 1 · Mode 1
            6 Personnel à découvert
            7 G 35T LL 33000 - 90300 · Z 610 m · TLE 2
            8 Circulaire · Rayon 100 m
            """
                .trimIndent(),
            """
            [DT AB1234 3/3]
            9 Grille · Gisement d'observ. 4800 · Observateur X 34550 Y 88800 Z 640
            10 Distance 800 m · Gisement 1600 · Niveau de protection 1
            11 Aussitôt prêt
            """
                .trimIndent(),
        )

    /** Every box filled with its longest values: the worst case for the size of the message. */
    val longest =
        FireRequest(
            origin = "O".repeat(FREE),
            recipient = "D".repeat(FREE),
            requestGdh = "112250 loc NOV 26",
            number = "ABCDE12345",
            fireType =
            FireType(
                lines = ImpactLine.entries.toSet(),
                firstRound = true,
                setUp = "S".repeat(FREE),
                offsetSetUp = "O".repeat(FREE),
                prepare = true,
                renew = "R".repeat(FREE),
                engagement = true,
            ),
            effect = Effect(true, true, true, true, true, illumination = "2", mixFirst = "4A", mixSecond = "4B"),
            observation = Observation("999", "999", "999", "999", "999", "999"),
            charge = Charge("4", "1", "3", "2", "3", "999", "9999", "9999"),
            targets =
            Targets(
                tanks = true,
                tanksMotion = Motion.STOPPED,
                lightArmour = true,
                lightArmourMotion = Motion.MOVING,
                pickup = true,
                pickupMotion = Motion.STOPPED,
                personnel = true,
                personnelCover = Cover.OPEN,
                artillery = true,
                artilleryPart = ArtilleryPart.OBSERVER,
                building = true,
                other = "B".repeat(FREE),
            ),
            location =
            Location(
                a = PolarLine("6399", "99999", "9999"),
                b = PolarLine("6399", "99999", "9999"),
                c = PolarLine("6399", "99999", "9999"),
                d = RectangularLine("999999", "9999999", "9999"),
                e = RectangularLine("999999", "9999999", "9999"),
                f = RectangularLine("999999", "9999999", "9999"),
                reference =
                ReferenceLine(
                    first = "R".repeat(FREE),
                    second = "S".repeat(FREE),
                    eastWest = Shift.WEST,
                    eastWestM = "99999",
                    northSouth = Shift.SOUTH,
                    northSouthM = "99999",
                    rightLeft = Shift.LEFT,
                    rightLeftM = "99999",
                    nearFar = Shift.FURTHER,
                    nearFarM = "99999",
                    upDown = Shift.LOWER,
                    upDownM = "99999",
                ),
                g = LruLine("35", "T", "LL", "33000", "90300", "9999"),
                h = LruLine("35", "T", "LL", "33000", "90300", "9999"),
                tle = "4",
            ),
            shape =
            Shape(
                kind = ShapeKind.SURFACE,
                centralPoint = "P".repeat(FREE),
                side = Side.LD,
                sideValue = "99999",
                bearing = "6399",
                front = "99999",
                depth = "99999",
                openness = Openness.CLOSED,
                centre = "C".repeat(FREE),
                radius = "99999",
            ),
            adjustment = Adjustment(AdjustMethod.MEAN_POINT_SHIFT, "6399", "999999", "9999999", "9999"),
            friends = Friends("99999", "6399", "3"),
            command =
            Command(
                atMyCommand = true,
                whenReady = true,
                whenReadyNlt = "23H59",
                synchro = "S".repeat(FREE),
                timeOnTarget = "23H59",
                limitedStart = "23H59",
                limitedEnd = "23H59",
                fireTime = "23H59",
                atCommandOf = "A".repeat(FREE),
            ),
        )

    /** The longest free text a box takes, in bytes. */
    const val FREE = 24
}
