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

/**
 * One line of box 7 shown in clear: its [kind] (POLAIRES, RECTANG., L R U, Rep, or TLE alone), its [letter], its text.
 */
data class LocationEntry(val kind: String, val letter: String?, val lines: List<String>)

/**
 * A request shown in clear on the recipient's phone (table D26 of the plan, validated box by box): the words of the
 * sheet, its codes followed by their legend (`Lanceurs 2 : Mo 120`), bearings followed by « m̶ » (mils). An empty box
 * gives no line; the screen shows it as `non rempli`.
 */
object FireRequestDisplay {

    /** « m » barred, the sheet's mark for mils. */
    const val MILS = "m̶"

    /** The lines of box [box] (1 to 11 but 7) in clear. */
    fun lines(request: FireRequest, box: Int): List<String> = when (box) {
        1 -> listOfNotNull(request.number.ifEmpty { null })

        2 ->
            fireType(request.fireType)
                .takeIf { it.isNotEmpty() }
                ?.let { listOf(it.joinToString(Sheet.SEPARATOR)) }
                .orEmpty()

        BOX_EFFECT -> effect(request.effect).map(::effectInClear)

        BOX_OBSERVATION -> observation(request.observation)

        BOX_CHARGE -> chargeInClear(request.charge)

        BOX_TARGETS -> targets(request.targets)

        BOX_SHAPE ->
            shape(request.shape)
                .map(::withMils)
                .takeIf { it.isNotEmpty() }
                ?.let { listOf(it.joinToString(Sheet.SEPARATOR)) }
                .orEmpty()

        BOX_ADJUSTMENT -> adjustment(request.adjustment).map { observerInClear(withMils(it)) }

        BOX_FRIENDS -> friendsInClear(request.friends)

        BOX_COMMAND -> command(request.command)

        else -> emptyList()
    }

    /** The lines of box 7 in clear, in the order of the sheet. */
    fun location(box: Location): List<LocationEntry> = buildList {
        listOf(ImpactLine.A to box.a, ImpactLine.B to box.b, ImpactLine.C to box.c).forEach { (letter, line) ->
            polar(letter, line)?.let {
                add(LocationEntry(Sheet.POLAR, letter.name, listOf(withMilsAll(it.substringAfter(" ")))))
            }
        }
        listOf(ImpactLine.D to box.d, ImpactLine.E to box.e, ImpactLine.F to box.f).forEach { (letter, line) ->
            rectangular(letter, line)?.let {
                add(LocationEntry(Sheet.RECTANGULAR, letter.name, listOf(it.substringAfter(" "))))
            }
        }
        reference(box.reference)?.let { add(LocationEntry(Sheet.REFERENCE, null, it.split(Sheet.SEPARATOR))) }
        listOf(ImpactLine.G to box.g, ImpactLine.H to box.h).forEach { (letter, line) ->
            lru(letter, line)?.let {
                add(LocationEntry(Sheet.LRU, letter.name, it.substringAfter(" ").split(Sheet.SEPARATOR)))
            }
        }
        if (box.tle.isNotEmpty()) add(LocationEntry(Sheet.TLE, null, listOf("${Sheet.TLE} ${box.tle}")))
    }

    private const val BOX_EFFECT = 3
    private const val BOX_OBSERVATION = 4
    private const val BOX_CHARGE = 5
    private const val BOX_TARGETS = 6
    private const val BOX_SHAPE = 8
    private const val BOX_ADJUSTMENT = 9
    private const val BOX_FRIENDS = 10
    private const val BOX_COMMAND = 11

    // The modes of box 5 that take a value: short delay (ms), airburst height and delay (m).
    private const val MODE_SHORT_DELAY = "2"
    private const val MODE_AIRBURST = "3"
    private const val MODE_DELAY = "4"

    private fun legend(codes: Map<String, String>, code: String): String = codes[code]?.let { "$code : $it" } ?: code

    /** `Éclairement 2 : IR`; `Panaché 1 Explo et 2 Fumigène`, as on the mock-up; `4A` is `4A ECL VISIB`. */
    private fun effectInClear(element: String): String {
        val illumination = valueOf(element, Sheet.ILLUMINATION)?.takeIf { it.isNotEmpty() }
        val mix = valueOf(element, Sheet.MIX)?.takeIf { it.isNotEmpty() }?.let { codePairOf(it, Sheet.AND) }
        return when {
            illumination != null -> "${Sheet.ILLUMINATION} ${legend(Sheet.ILLUMINATION_CODES, illumination)}"

            mix != null ->
                "${Sheet.MIX} " + mix.toList().filter { it.isNotEmpty() }.joinToString(" ${Sheet.AND} ") { mixCode(it) }

            else -> element
        }
    }

    private fun mixCode(code: String): String {
        val legends =
            listOfNotNull(
                Sheet.MIX_CODES[code.take(1)],
                Sheet.MIX_ILLUMINATION_CODES[code.drop(1)].takeIf { code.length > 1 },
            )
        return (listOf(code) + legends).joinToString(" ")
    }

    /** `Lanceurs 2 : Mo 120`, `Trajectoire 1 : Plongeante / 3 : Nominale`, each mode with its delay or height. */
    private fun chargeInClear(box: Charge): List<String> {
        val delays =
            mapOf(
                MODE_SHORT_DELAY to (box.shortDelayMs to Sheet.MILLISECONDS),
                MODE_AIRBURST to (box.airburstM to Sheet.METRES),
                MODE_DELAY to (box.delayM to Sheet.METRES),
            )
        fun mode(code: String): String {
            val (value, unit) = delays[code] ?: ("" to "")
            return legend(Sheet.MODE_CODES, code) + if (value.isEmpty()) "" else " $value $unit"
        }
        val modes = listOf(box.modeFirst, box.modeSecond).filter { it.isNotEmpty() }
        val trajectories = listOf(box.trajectoryFirst, box.trajectorySecond).filter { it.isNotEmpty() }
        val loose =
            listOfNotNull(
                box.shortDelayMs
                    .takeIf { it.isNotEmpty() && MODE_SHORT_DELAY !in modes }
                    ?.let { "${Sheet.SHORT_DELAY} $it ${Sheet.MILLISECONDS}" },
                box.airburstM
                    .takeIf { it.isNotEmpty() && MODE_AIRBURST !in modes }
                    ?.let { "${Sheet.AIRBURST} $it ${Sheet.METRES}" },
                box.delayM
                    .takeIf { it.isNotEmpty() && MODE_DELAY !in modes }
                    ?.let { "${Sheet.DELAY} $it ${Sheet.METRES}" },
            )
        return listOfNotNull(
            box.launcher.takeIf { it.isNotEmpty() }?.let { "${Sheet.LAUNCHERS} ${legend(Sheet.LAUNCHER_CODES, it)}" },
            trajectories
                .takeIf { it.isNotEmpty() }
                ?.let { codes ->
                    "${Sheet.TRAJECTORY} " + codes.joinToString(" / ") { legend(Sheet.TRAJECTORY_CODES, it) }
                },
            modes
                .takeIf { it.isNotEmpty() }
                ?.let { codes -> "${Sheet.MODE} " + codes.joinToString(" ${Sheet.AND} ") { mode(it) } },
        ) + loose
    }

    /** `Distance 800 m · Gisement 1600 m̶`, then `Niveau de protection 1 : à découvert`. */
    private fun friendsInClear(box: Friends): List<String> {
        val first =
            listOfNotNull(
                box.distance.takeIf { it.isNotEmpty() }?.let { "${Sheet.DISTANCE} $it ${Sheet.METRES}" },
                box.bearing.takeIf { it.isNotEmpty() }?.let { withMils("${Sheet.BEARING} $it") },
            )
        return listOfNotNull(
            first.takeIf { it.isNotEmpty() }?.joinToString(Sheet.SEPARATOR),
            box.protection
                .takeIf { it.isNotEmpty() }
                ?.let { "${Sheet.PROTECTION} ${legend(Sheet.PROTECTION_CODES, it)}" },
        )
    }

    /** A bearing in mils gets its mark; one given as N, O, E or S does not. */
    private fun withMils(element: String): String {
        val label = listOf(Sheet.OBSERVATION_BEARING, Sheet.BEARING).firstOrNull { valueOf(element, it) != null }
        val value = label?.let { valueOf(element, it) }
        return if (value != null && value.isNotEmpty() && value.all { it.isDigit() }) "$element $MILS" else element
    }

    /** Polar lines: `G 1600 m̶ · D 2500 m · S 20 m`. */
    private fun withMilsAll(line: String): String = line.split(Sheet.SEPARATOR).joinToString(Sheet.SEPARATOR) {
        if (valueOf(it, Sheet.BEARING_LETTER)?.all { c -> c.isDigit() } == true) "$it $MILS" else it
    }

    /** `Observateur X 34550 · Y 88800 · Z 640`, as on the mock-up. */
    private fun observerInClear(element: String): String = if (element.startsWith("${Sheet.OBSERVER} ")) {
        Sheet.OBSERVER +
            " " +
            element
                .removePrefix("${Sheet.OBSERVER} ")
                .replace(" ${Sheet.Y} ", "${Sheet.SEPARATOR}${Sheet.Y} ")
                .replace(" ${Sheet.Z} ", "${Sheet.SEPARATOR}${Sheet.Z} ")
    } else {
        element
    }
}
