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

internal fun fireType(box: FireType): List<String> {
    val elements =
        listOfNotNull(
            flag(Sheet.FIRST_ROUND, box.firstRound),
            ticked(Sheet.SET_UP, box.setUp),
            ticked(Sheet.OFFSET_SET_UP, box.offsetSetUp),
            flag(Sheet.PREPARE, box.prepare),
            ticked(Sheet.RENEW, box.renew),
            flag(Sheet.ENGAGEMENT, box.engagement),
        )
    val letters = box.lines.sorted().joinToString(" ") { it.name }
    return when {
        letters.isEmpty() -> elements
        elements.isEmpty() -> listOf(letters)
        else -> listOf("$letters ${elements.first()}") + elements.drop(1)
    }
}

internal fun effect(box: Effect): List<String> = listOfNotNull(
    flag(Sheet.NEUTRALISATION, box.neutralisation),
    flag(Sheet.DESTRUCTION, box.destruction),
    flag(Sheet.STOP, box.stop),
    flag(Sheet.WARNING, box.warning),
    flag(Sheet.BLINDING, box.blinding),
    ticked(Sheet.ILLUMINATION, box.illumination),
    box.mixFirst?.let { codePair(Sheet.MIX, it, box.mixSecond, Sheet.AND) ?: Sheet.MIX },
)

internal fun observation(box: Observation): List<String> = listOfNotNull(
    slashPair(Sheet.SHOTS, box.shotsFirst, box.shotsSecond, null),
    slashPair(Sheet.DURATION, box.durationFirst, box.durationSecond, Sheet.MINUTES),
    slashPair(Sheet.INTERVAL, box.intervalFirst, box.intervalSecond, Sheet.SECONDS),
)

internal fun charge(box: Charge): List<String> = listOfNotNull(
    labelled(Sheet.LAUNCHERS, box.launcher),
    codePair(Sheet.TRAJECTORY, box.trajectoryFirst, box.trajectorySecond, "/"),
    codePair(Sheet.MODE, box.modeFirst, box.modeSecond, Sheet.AND),
    labelled(Sheet.SHORT_DELAY, box.shortDelayMs, Sheet.MILLISECONDS),
    labelled(Sheet.AIRBURST, box.airburstM, Sheet.METRES),
    labelled(Sheet.DELAY, box.delayM, Sheet.METRES),
)

internal fun targets(box: Targets): List<String> = listOfNotNull(
    target(Sheet.TANKS, box.tanks, box.tanksMotion?.label),
    target(Sheet.LIGHT_ARMOUR, box.lightArmour, box.lightArmourMotion?.label),
    target(Sheet.PICKUP, box.pickup, box.pickupMotion?.label),
    target(Sheet.PERSONNEL, box.personnel, box.personnelCover?.label),
    target(Sheet.ARTILLERY, box.artillery, box.artilleryPart?.label),
    flag(Sheet.BUILDING, box.building),
    cleanText(box.other).ifEmpty { null },
)

internal fun shape(box: Shape): List<String> = listOfNotNull(
    box.kind?.label,
    labelled(Sheet.CENTRAL_POINT, box.centralPoint),
    box.side?.let { labelled(it.label, box.sideValue) ?: it.label },
    labelled(Sheet.BEARING, box.bearing),
    labelled(Sheet.FRONT, box.front, Sheet.METRES),
    labelled(Sheet.DEPTH, box.depth, Sheet.METRES),
    box.openness?.label,
    labelled(Sheet.CENTRE, box.centre),
    labelled(Sheet.RADIUS, box.radius, Sheet.METRES),
)

internal fun adjustment(box: Adjustment): List<String> {
    val observer =
        listOfNotNull(
            labelled(Sheet.X, box.observerX),
            labelled(Sheet.Y, box.observerY),
            labelled(Sheet.Z, box.observerZ),
        )
    return listOfNotNull(
        box.method?.label,
        labelled(Sheet.OBSERVATION_BEARING, box.observationBearing),
        observer.takeIf { it.isNotEmpty() }?.let { "${Sheet.OBSERVER} ${it.joinToString(" ")}" },
    )
}

internal fun friends(box: Friends): List<String> = listOfNotNull(
    labelled(Sheet.DISTANCE, box.distance, Sheet.METRES),
    labelled(Sheet.BEARING, box.bearing),
    labelled(Sheet.PROTECTION, box.protection),
)

internal fun command(box: Command): List<String> = listOfNotNull(
    flag(Sheet.AT_MY_COMMAND, box.atMyCommand),
    flag(Sheet.WHEN_READY, box.whenReady),
    ticked(Sheet.WHEN_READY_NLT, box.whenReadyNlt),
    ticked(Sheet.SYNCHRO, box.synchro),
    ticked(Sheet.TIME_ON_TARGET, box.timeOnTarget),
    box.limitedStart?.let { start ->
        listOfNotNull(
            Sheet.TIME_ON_TARGET_LIMITED,
            labelled(Sheet.START, start),
            labelled(Sheet.END, box.limitedEnd),
        )
            .joinToString(" ")
    },
    ticked(Sheet.FIRE_TIME, box.fireTime),
    ticked(Sheet.AT_COMMAND_OF, box.atCommandOf),
)
