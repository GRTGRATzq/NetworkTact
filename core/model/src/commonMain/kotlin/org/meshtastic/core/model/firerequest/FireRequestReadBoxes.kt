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

import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

// Reading boxes 2 to 6 and 8 to 11 back: the inverse of FireRequestBoxes.kt. Each element is read by the first rule
// that knows it; one that none knows goes to [unread] and is shown as it came.

/** Folds [elements] into [start]: [read] gives the box with the element in it, or null when it does not know it. */
internal fun <T> fold(elements: List<String>, unread: MutableList<String>, start: T, read: (T, String) -> T?): T =
    elements.fold(start) { box, element -> read(box, element) ?: box.also { unread += element } }

private val LETTERS = Regex("""^([A-H](?: [A-H])*)(?: (.+))?$""")

internal fun readFireType(elements: List<String>, unread: MutableList<String>): FireType {
    val first = elements.firstOrNull()?.let { LETTERS.find(it) }
    val start =
        FireType(lines = first?.groupValues?.get(1)?.split(" ")?.map { ImpactLine.valueOf(it) }?.toSet().orEmpty())
    val rest = if (first == null) elements else listOfNotNull(first.groupValues[2].ifEmpty { null }) + elements.drop(1)
    return fold(rest, unread, start) { box, element ->
        when {
            element == Sheet.FIRST_ROUND -> box.copy(firstRound = true)

            element == Sheet.PREPARE -> box.copy(prepare = true)

            element == Sheet.ENGAGEMENT -> box.copy(engagement = true)

            else ->
                valueOf(element, Sheet.SET_UP)?.let { box.copy(setUp = it) }
                    ?: valueOf(element, Sheet.OFFSET_SET_UP)?.let { box.copy(offsetSetUp = it) }
                    ?: valueOf(element, Sheet.RENEW)?.let { box.copy(renew = it) }
        }
    }
}

internal fun readEffect(elements: List<String>, unread: MutableList<String>): Effect =
    fold(elements, unread, Effect()) { box, element ->
        when (element) {
            Sheet.NEUTRALISATION -> box.copy(neutralisation = true)

            Sheet.DESTRUCTION -> box.copy(destruction = true)

            Sheet.STOP -> box.copy(stop = true)

            Sheet.WARNING -> box.copy(warning = true)

            Sheet.BLINDING -> box.copy(blinding = true)

            else ->
                valueOf(element, Sheet.ILLUMINATION)?.let { box.copy(illumination = it) }
                    ?: valueOf(element, Sheet.MIX)?.let {
                        val (first, second) = codePairOf(it, Sheet.AND)
                        box.copy(mixFirst = first, mixSecond = second)
                    }
        }
    }

internal fun readObservation(elements: List<String>, unread: MutableList<String>): Observation =
    fold(elements, unread, Observation()) { box, element ->
        slashPairOf(element, Sheet.SHOTS, null)?.let { (a, b) -> box.copy(shotsFirst = a, shotsSecond = b) }
            ?: slashPairOf(element, Sheet.DURATION, Sheet.MINUTES)?.let { (a, b) ->
                box.copy(durationFirst = a, durationSecond = b)
            }
            ?: slashPairOf(element, Sheet.INTERVAL, Sheet.SECONDS)?.let { (a, b) ->
                box.copy(intervalFirst = a, intervalSecond = b)
            }
    }

internal fun readCharge(elements: List<String>, unread: MutableList<String>): Charge =
    fold(elements, unread, Charge()) { box, element ->
        valueOf(element, Sheet.LAUNCHERS)?.let { box.copy(launcher = it) }
            ?: valueOf(element, Sheet.TRAJECTORY)?.let {
                val (first, second) = codePairOf(it, "/")
                box.copy(trajectoryFirst = first, trajectorySecond = second)
            }
            ?: valueOf(element, Sheet.MODE)?.let {
                val (first, second) = codePairOf(it, Sheet.AND)
                box.copy(modeFirst = first, modeSecond = second)
            }
            ?: measureOf(element, Sheet.SHORT_DELAY, Sheet.MILLISECONDS)?.let { box.copy(shortDelayMs = it) }
            ?: measureOf(element, Sheet.AIRBURST, Sheet.METRES)?.let { box.copy(airburstM = it) }
            ?: measureOf(element, Sheet.DELAY, Sheet.METRES)?.let { box.copy(delayM = it) }
    }

/** A target read back, with its [state] (null when none is written). */
private class Target<S>(val state: S?)

/** `label` or `label state`, with the state among [states]; null when [element] is another element. */
private fun <S> targetOf(element: String, label: String, states: Array<S>, name: (S) -> String): Target<S>? {
    val value = valueOf(element, label) ?: return null
    return if (value.isEmpty()) Target(null) else states.firstOrNull { name(it) == value }?.let { Target(it) }
}

internal fun readTargets(elements: List<String>, unread: MutableList<String>): Targets =
    fold(elements, unread, Targets()) { box, element ->
        targetOf(element, Sheet.TANKS, Motion.entries.toTypedArray()) { it.label }
            ?.let { box.copy(tanks = true, tanksMotion = it.state) }
            ?: targetOf(element, Sheet.LIGHT_ARMOUR, Motion.entries.toTypedArray()) { it.label }
                ?.let { box.copy(lightArmour = true, lightArmourMotion = it.state) }
            ?: targetOf(element, Sheet.PICKUP, Motion.entries.toTypedArray()) { it.label }
                ?.let { box.copy(pickup = true, pickupMotion = it.state) }
            ?: targetOf(element, Sheet.PERSONNEL, Cover.entries.toTypedArray()) { it.label }
                ?.let { box.copy(personnel = true, personnelCover = it.state) }
            ?: targetOf(element, Sheet.ARTILLERY, ArtilleryPart.entries.toTypedArray()) { it.label }
                ?.let { box.copy(artillery = true, artilleryPart = it.state) }
            ?: box.takeIf { element == Sheet.BUILDING }?.copy(building = true)
            // The free box of « Bâtiment ou [ ] » is whatever else is written, once.
            ?: box.takeIf { it.other.isEmpty() }?.copy(other = element)
    }

internal fun readShape(elements: List<String>, unread: MutableList<String>): Shape =
    fold(elements, unread, Shape()) { box, element ->
        ShapeKind.entries.firstOrNull { it.label == element }?.let { box.copy(kind = it) }
            ?: Openness.entries.firstOrNull { it.label == element }?.let { box.copy(openness = it) }
            ?: Side.entries.firstNotNullOfOrNull { side ->
                valueOf(element, side.label)?.let { box.copy(side = side, sideValue = it) }
            }
            ?: valueOf(element, Sheet.CENTRAL_POINT)?.let { box.copy(centralPoint = it) }
            ?: valueOf(element, Sheet.BEARING)?.let { box.copy(bearing = it) }
            ?: measureOf(element, Sheet.FRONT, Sheet.METRES)?.let { box.copy(front = it) }
            ?: measureOf(element, Sheet.DEPTH, Sheet.METRES)?.let { box.copy(depth = it) }
            ?: valueOf(element, Sheet.CENTRE)?.let { box.copy(centre = it) }
            ?: measureOf(element, Sheet.RADIUS, Sheet.METRES)?.let { box.copy(radius = it) }
    }

private val OBSERVER = Regex("""^Observateur(?: X (\S+))?(?: Y (\S+))?(?: Z (\S+))?$""")

internal fun readAdjustment(elements: List<String>, unread: MutableList<String>): Adjustment =
    fold(elements, unread, Adjustment()) { box, element ->
        AdjustMethod.entries.firstOrNull { it.label == element }?.let { box.copy(method = it) }
            ?: valueOf(element, Sheet.OBSERVATION_BEARING)?.let { box.copy(observationBearing = it) }
            ?: OBSERVER.find(element)?.destructured?.let { (x, y, z) ->
                box.copy(observerX = x, observerY = y, observerZ = z)
            }
    }

internal fun readFriends(elements: List<String>, unread: MutableList<String>): Friends =
    fold(elements, unread, Friends()) { box, element ->
        measureOf(element, Sheet.DISTANCE, Sheet.METRES)?.let { box.copy(distance = it) }
            ?: valueOf(element, Sheet.BEARING)?.let { box.copy(bearing = it) }
            ?: valueOf(element, Sheet.PROTECTION)?.let { box.copy(protection = it) }
    }

private val LIMITED = Regex("""^HSO limité(?: début (\S+))?(?: fin (\S+))?$""")

internal fun readCommand(elements: List<String>, unread: MutableList<String>): Command =
    fold(elements, unread, Command()) { box, element ->
        when (element) {
            Sheet.AT_MY_COMMAND -> box.copy(atMyCommand = true)

            Sheet.WHEN_READY -> box.copy(whenReady = true)

            else ->
                valueOf(element, Sheet.WHEN_READY_NLT)?.let { box.copy(whenReadyNlt = it) }
                    ?: valueOf(element, Sheet.SYNCHRO)?.let { box.copy(synchro = it) }
                    ?: valueOf(element, Sheet.TIME_ON_TARGET)?.let { box.copy(timeOnTarget = it) }
                    ?: LIMITED.find(element)?.groupValues?.let { (_, start, end) ->
                        box.copy(limitedStart = start, limitedEnd = end)
                    }
                    ?: valueOf(element, Sheet.FIRE_TIME)?.let { box.copy(fireTime = it) }
                    ?: valueOf(element, Sheet.AT_COMMAND_OF)?.let { box.copy(atCommandOf = it) }
        }
    }
