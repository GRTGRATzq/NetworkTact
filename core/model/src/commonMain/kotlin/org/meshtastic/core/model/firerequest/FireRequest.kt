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

import kotlinx.serialization.Serializable

/**
 * A fire request as the operator fills in the sheet: the header and boxes 1 to 11. Every value is the text typed in a
 * box, `""` when the box is empty; a box with a tick and a value (`Mettre en place [ ]`) is a nullable text, null while
 * it is not ticked. No box is mandatory. The words written for each choice are those of [FireRequestSheet].
 */
@Serializable
data class FireRequest(
    val origin: String = "",
    /** The recipient as written in the message (`CDS TIR`); the node it is sent to is chosen apart. */
    val recipient: String = "",
    /** The request's date-time group as typed (`112250 loc NOV 26`). */
    val requestGdh: String = "",
    /** Box 1: letters and digits. */
    val number: String = "",
    val fireType: FireType = FireType(),
    val effect: Effect = Effect(),
    val observation: Observation = Observation(),
    val charge: Charge = Charge(),
    val targets: Targets = Targets(),
    val location: Location = Location(),
    val shape: Shape = Shape(),
    val adjustment: Adjustment = Adjustment(),
    val friends: Friends = Friends(),
    val command: Command = Command(),
)

/** The lines of box 7, also ticked in box 2: A to C polar, D to F rectangular, G and H L R U. */
@Serializable
enum class ImpactLine {
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
}

/** Box 2. */
@Serializable
data class FireType(
    val lines: Set<ImpactLine> = emptySet(),
    val firstRound: Boolean = false,
    val setUp: String? = null,
    val offsetSetUp: String? = null,
    val prepare: Boolean = false,
    val renew: String? = null,
    val engagement: Boolean = false,
)

/** Box 3. [illumination] is code 1 or 2; [mixFirst] (ticked when not null) and [mixSecond] are codes 1 to 5, 4A, 4B. */
@Serializable
data class Effect(
    val neutralisation: Boolean = false,
    val destruction: Boolean = false,
    val stop: Boolean = false,
    val warning: Boolean = false,
    val blinding: Boolean = false,
    val illumination: String? = null,
    val mixFirst: String? = null,
    val mixSecond: String = "",
)

/** Box 4: each line holds two values either side of « / ». */
@Serializable
data class Observation(
    val shotsFirst: String = "",
    val shotsSecond: String = "",
    val durationFirst: String = "",
    val durationSecond: String = "",
    val intervalFirst: String = "",
    val intervalSecond: String = "",
)

/** Box 5: codes of the sheet, and the values of « Court retard … ms », « Fusant … m », « Retard … m ». */
@Serializable
data class Charge(
    val launcher: String = "",
    val trajectoryFirst: String = "",
    val trajectorySecond: String = "",
    val modeFirst: String = "",
    val modeSecond: String = "",
    val shortDelayMs: String = "",
    val airburstM: String = "",
    val delayM: String = "",
)

/** « en mouvement / à l'arrêt », for tanks, light armour and pick-ups. */
@Serializable
enum class Motion(val label: String) {
    MOVING("en mouvement"),
    STOPPED("à l'arrêt"),
}

/** « à découvert / abrité », for personnel. */
@Serializable
enum class Cover(val label: String) {
    OPEN("à découvert"),
    SHELTERED("abrité"),
}

/** « observateur / pièces », for artillery. */
@Serializable
enum class ArtilleryPart(val label: String) {
    OBSERVER("observateur"),
    PIECES("pièces"),
}

/** Box 6. A state given also ticks its target. [other] is the free box of « Bâtiment ou [ ] ». */
@Serializable
data class Targets(
    val tanks: Boolean = false,
    val tanksMotion: Motion? = null,
    val lightArmour: Boolean = false,
    val lightArmourMotion: Motion? = null,
    val pickup: Boolean = false,
    val pickupMotion: Motion? = null,
    val personnel: Boolean = false,
    val personnelCover: Cover? = null,
    val artillery: Boolean = false,
    val artilleryPart: ArtilleryPart? = null,
    val building: Boolean = false,
    val other: String = "",
)

/** A polar line of box 7: bearing G (mils), distance D and site S (metres). */
@Serializable data class PolarLine(val bearing: String = "", val distance: String = "", val site: String = "")

/** A rectangular line of box 7: X, Y and Z (metres). */
@Serializable data class RectangularLine(val x: String = "", val y: String = "", val z: String = "")

/**
 * An L R U line of box 7, an MGRS coordinate written `35T LL 33000 - 90300`: UTM zone, latitude band, 100 km square, X
 * (easting) and Y (northing) on five digits; Z (altitude, metres) is optional.
 */
@Serializable
data class LruLine(
    val zone: String = "",
    val band: String = "",
    val square: String = "",
    val x: String = "",
    val y: String = "",
    val z: String = "",
)

/** One of the two words of a « Plus à l'est - ouest » choice of the Rep line. */
@Serializable
enum class Shift(val label: String) {
    EAST("Plus à l'est"),
    WEST("Plus à l'ouest"),
    NORTH("Plus au nord"),
    SOUTH("Plus au sud"),
    RIGHT("Plus à Dte"),
    LEFT("Plus à Gche"),
    NEARER("Plus près"),
    FURTHER("Plus loin"),
    HIGHER("Plus haut"),
    LOWER("Plus bas"),
}

/** The Rep line of box 7: two free boxes, then shifts in metres (the direction is chosen, then the value typed). */
@Serializable
data class ReferenceLine(
    val first: String = "",
    val second: String = "",
    val eastWest: Shift? = null,
    val eastWestM: String = "",
    val northSouth: Shift? = null,
    val northSouthM: String = "",
    val rightLeft: Shift? = null,
    val rightLeftM: String = "",
    val nearFar: Shift? = null,
    val nearFarM: String = "",
    val upDown: Shift? = null,
    val upDownM: String = "",
)

/** Box 7. [tle] is the location accuracy, 1 to 4. */
@Serializable
data class Location(
    val a: PolarLine = PolarLine(),
    val b: PolarLine = PolarLine(),
    val c: PolarLine = PolarLine(),
    val d: RectangularLine = RectangularLine(),
    val e: RectangularLine = RectangularLine(),
    val f: RectangularLine = RectangularLine(),
    val reference: ReferenceLine = ReferenceLine(),
    val g: LruLine = LruLine(),
    val h: LruLine = LruLine(),
    val tle: String = "",
)

/** The impact shapes of box 8. */
@Serializable
enum class ShapeKind(val label: String) {
    CONVERGENT("Convergent"),
    LINEAR("Linéaire"),
    SURFACE("Surfacique"),
    CIRCULAR("Circulaire"),
}

/** « LG [ ] ou LD [ ] ». */
@Serializable
enum class Side(val label: String) {
    LG("LG"),
    LD("LD"),
}

/** « Ouvert ou fermé ». */
@Serializable
enum class Openness(val label: String) {
    OPEN("Ouvert"),
    CLOSED("Fermé"),
}

/** Box 8. LG and LD have no unit, as on the sheet. */
@Serializable
data class Shape(
    val kind: ShapeKind? = null,
    val centralPoint: String = "",
    val side: Side? = null,
    val sideValue: String = "",
    val bearing: String = "",
    val front: String = "",
    val depth: String = "",
    val openness: Openness? = null,
    val centre: String = "",
    val radius: String = "",
)

/** « Grille ou Déplacement du point moyen ». */
@Serializable
enum class AdjustMethod(val label: String) {
    GRID("Grille"),
    MEAN_POINT_SHIFT("Déplacement du point moyen"),
}

/** Box 9: the observer's X, Y and Z are typed by the operator, never filled in. */
@Serializable
data class Adjustment(
    val method: AdjustMethod? = null,
    val observationBearing: String = "",
    val observerX: String = "",
    val observerY: String = "",
    val observerZ: String = "",
)

/** Box 10. [bearing] is in mils, or one of N O E S; [protection] is code 1 to 3. */
@Serializable data class Friends(val distance: String = "", val bearing: String = "", val protection: String = "")

/** Box 11, several choices allowed. Times are written as typed (`14H30`), with no time zone, as on the sheet. */
@Serializable
data class Command(
    val atMyCommand: Boolean = false,
    val whenReady: Boolean = false,
    val whenReadyNlt: String? = null,
    val synchro: String? = null,
    val timeOnTarget: String? = null,
    val limitedStart: String? = null,
    val limitedEnd: String = "",
    val fireTime: String? = null,
    val atCommandOf: String? = null,
)
