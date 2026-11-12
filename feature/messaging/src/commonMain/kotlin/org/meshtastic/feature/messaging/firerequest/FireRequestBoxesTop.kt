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
package org.meshtastic.feature.messaging.firerequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.firerequest.ArtilleryPart
import org.meshtastic.core.model.firerequest.Charge
import org.meshtastic.core.model.firerequest.Cover
import org.meshtastic.core.model.firerequest.Effect
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.FireType
import org.meshtastic.core.model.firerequest.ImpactLine
import org.meshtastic.core.model.firerequest.Motion
import org.meshtastic.core.model.firerequest.Observation
import org.meshtastic.core.model.firerequest.Targets
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_empty_box
import org.meshtastic.core.resources.firereq_number_help
import org.meshtastic.core.resources.firereq_type_help
import org.meshtastic.core.model.firerequest.FireRequestSheet as Sheet

// Boxes 1 to 6 of the form, in the order and with the words of the sheet.

@Composable
internal fun NumberBox(request: FireRequest, edit: ((FireRequest) -> FireRequest) -> Unit) {
    BoxCard(1) {
        SheetField(
            label = Sheet.BOX_TITLES[0],
            value = request.number,
            onChange = { value -> edit { it.copy(number = value) } },
            filter = ::numberInput,
            width = 200.dp,
            isError = request.number.isEmpty(),
        )
        Help(stringResource(Res.string.firereq_number_help))
    }
}

@Composable
internal fun FireTypeBox(box: FireType, edit: (FireType) -> Unit) {
    BoxCard(2) {
        Help(stringResource(Res.string.firereq_type_help))
        Choices {
            ImpactLine.entries.forEach { line ->
                Choice(line.name, line in box.lines) {
                    edit(box.copy(lines = if (line in box.lines) box.lines - line else box.lines + line))
                }
            }
        }
        Choices {
            Choice(Sheet.FIRST_ROUND, box.firstRound) { edit(box.copy(firstRound = !box.firstRound)) }
            Choice(Sheet.PREPARE, box.prepare) { edit(box.copy(prepare = !box.prepare)) }
            Choice(Sheet.ENGAGEMENT, box.engagement) { edit(box.copy(engagement = !box.engagement)) }
        }
        ChoiceWithValue(Sheet.SET_UP, box.setUp) { edit(box.copy(setUp = it)) }
        ChoiceWithValue(Sheet.OFFSET_SET_UP, box.offsetSetUp) { edit(box.copy(offsetSetUp = it)) }
        ChoiceWithValue(Sheet.RENEW, box.renew) { edit(box.copy(renew = it)) }
    }
}

@Composable
internal fun EffectBox(box: Effect, edit: (Effect) -> Unit) {
    BoxCard(BOX_EFFECT) {
        Choices {
            Choice(Sheet.NEUTRALISATION, box.neutralisation) { edit(box.copy(neutralisation = !box.neutralisation)) }
            Choice(Sheet.DESTRUCTION, box.destruction) { edit(box.copy(destruction = !box.destruction)) }
            Choice(Sheet.STOP, box.stop) { edit(box.copy(stop = !box.stop)) }
            Choice(Sheet.WARNING, box.warning) { edit(box.copy(warning = !box.warning)) }
            Choice(Sheet.BLINDING, box.blinding) { edit(box.copy(blinding = !box.blinding)) }
        }
        Choice(Sheet.ILLUMINATION, box.illumination != null) {
            edit(box.copy(illumination = if (box.illumination == null) "" else null))
        }
        box.illumination?.let { code ->
            CodeChoice(Sheet.ILLUMINATION, Sheet.ILLUMINATION_CODES, code) { edit(box.copy(illumination = it)) }
        }
        Choice(Sheet.MIX, box.mixFirst != null) {
            edit(if (box.mixFirst == null) box.copy(mixFirst = "") else box.copy(mixFirst = null, mixSecond = ""))
        }
        box.mixFirst?.let { first ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WordField(Sheet.MIX, first, { edit(box.copy(mixFirst = it.uppercase())) }, width = 80.dp)
                Text(Sheet.AND)
                WordField("", box.mixSecond, { edit(box.copy(mixSecond = it.uppercase())) }, width = 80.dp)
            }
            Help(mixLegend())
        }
    }
}

/**
 * « 1. Explo ou 2. Fumigène, ou 3 : ACED ou 4 : ECL et A : VISIB ou B : IR ou 5 : Semonce », from the sheet's codes.
 */
private fun mixLegend(): String =
    (Sheet.MIX_CODES + Sheet.MIX_ILLUMINATION_CODES).entries.joinToString(" · ") { (code, legend) -> "$code : $legend" }

@Composable
internal fun ObservationBox(box: Observation, edit: (Observation) -> Unit) {
    BoxCard(BOX_OBSERVATION) {
        SlashPair(Sheet.SHOTS, box.shotsFirst, box.shotsSecond, null) { a, b ->
            edit(box.copy(shotsFirst = a, shotsSecond = b))
        }
        SlashPair(Sheet.DURATION, box.durationFirst, box.durationSecond, Sheet.MINUTES) { a, b ->
            edit(box.copy(durationFirst = a, durationSecond = b))
        }
        SlashPair(Sheet.INTERVAL, box.intervalFirst, box.intervalSecond, Sheet.SECONDS) { a, b ->
            edit(box.copy(intervalFirst = a, intervalSecond = b))
        }
        if (box == Observation()) Help(stringResource(Res.string.firereq_empty_box, BOX_OBSERVATION))
    }
}

/** `label [ ] / [ ] unit`, as boxes 4 and 5 write two values. */
@Composable
private fun SlashPair(label: String, first: String, second: String, unit: String?, onChange: (String, String) -> Unit) {
    Column {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("", first, { onChange(it, second) })
            Text("/")
            NumberField("", second, { onChange(first, it) }, unit = unit)
        }
    }
}

@Composable
internal fun ChargeBox(box: Charge, edit: (Charge) -> Unit) {
    BoxCard(BOX_CHARGE) {
        CodeChoice(Sheet.LAUNCHERS, Sheet.LAUNCHER_CODES, box.launcher) { edit(box.copy(launcher = it)) }
        CodeChoice(Sheet.TRAJECTORY, Sheet.TRAJECTORY_CODES, box.trajectoryFirst) {
            edit(box.copy(trajectoryFirst = it))
        }
        CodeChoice("${Sheet.TRAJECTORY} /", Sheet.TRAJECTORY_CODES, box.trajectorySecond) {
            edit(box.copy(trajectorySecond = it))
        }
        CodeChoice(Sheet.MODE, Sheet.MODE_CODES, box.modeFirst) { edit(box.copy(modeFirst = it)) }
        CodeChoice("${Sheet.MODE} ${Sheet.AND}", Sheet.MODE_CODES, box.modeSecond) { edit(box.copy(modeSecond = it)) }
        NumberField(
            Sheet.SHORT_DELAY,
            box.shortDelayMs,
            { edit(box.copy(shortDelayMs = it)) },
            unit = Sheet.MILLISECONDS,
        )
        NumberField(Sheet.AIRBURST, box.airburstM, { edit(box.copy(airburstM = it)) }, unit = Sheet.METRES)
        NumberField(Sheet.DELAY, box.delayM, { edit(box.copy(delayM = it)) }, unit = Sheet.METRES)
    }
}

@Composable
internal fun TargetsBox(box: Targets, edit: (Targets) -> Unit) {
    BoxCard(BOX_TARGETS) {
        Target(Sheet.TANKS, box.tanks, box.tanksMotion, Motion.entries, { it.label }) { ticked, state ->
            edit(box.copy(tanks = ticked, tanksMotion = state))
        }
        Target(Sheet.LIGHT_ARMOUR, box.lightArmour, box.lightArmourMotion, Motion.entries, { it.label }) {
                ticked,
                state,
            ->
            edit(box.copy(lightArmour = ticked, lightArmourMotion = state))
        }
        Target(Sheet.PICKUP, box.pickup, box.pickupMotion, Motion.entries, { it.label }) { ticked, state ->
            edit(box.copy(pickup = ticked, pickupMotion = state))
        }
        Target(Sheet.PERSONNEL, box.personnel, box.personnelCover, Cover.entries, { it.label }) { ticked, state ->
            edit(box.copy(personnel = ticked, personnelCover = state))
        }
        Target(Sheet.ARTILLERY, box.artillery, box.artilleryPart, ArtilleryPart.entries, { it.label }) {
                ticked,
                state,
            ->
            edit(box.copy(artillery = ticked, artilleryPart = state))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(Sheet.BUILDING, box.building) { edit(box.copy(building = !box.building)) }
            Text(Sheet.OR)
            SheetField("", box.other, { edit(box.copy(other = it)) }, width = 180.dp)
        }
    }
}

/** A target with its state: the state ticks the target, unticking the target clears its state. */
@Composable
private fun <S> Target(
    label: String,
    ticked: Boolean,
    state: S?,
    states: List<S>,
    name: (S) -> String,
    onChange: (Boolean, S?) -> Unit,
) {
    Choices {
        Choice(label, ticked || state != null) {
            if (ticked || state != null) onChange(false, null) else onChange(true, null)
        }
        states.forEach { option ->
            Choice(name(option), state == option) { onChange(true, if (state == option) null else option) }
        }
    }
}
