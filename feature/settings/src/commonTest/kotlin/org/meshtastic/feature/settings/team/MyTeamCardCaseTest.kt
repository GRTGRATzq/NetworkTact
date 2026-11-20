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
package org.meshtastic.feature.settings.team

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.model.team.TeamSuffix
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * D9: Drone carried « Drone [Obs] »; after the list PC;OBS;TIR was adopted, choosing OBS left « [Obs] ». The team the
 * radio declares is the exact text of its suffix: one that differs from a team of the list only by case is not that
 * team, so choosing the team of the list renames the radio.
 */
@OptIn(ExperimentalTestApi::class)
class MyTeamCardCaseTest {

    private val roster =
        TeamRosterRecord(teams = listOf("PC", "OBS", "TIR"), source = TeamRosterRecord.Source.MANUAL, timeSeconds = 0)

    private fun ComposeUiTest.showCard(longName: String, chosen: MutableList<String?>) {
        val state =
            TeamsUiState(
                roster = roster,
                myLongName = longName,
                myTeam = TeamSuffix.teamOf(longName),
                block = null,
                connected = true,
            )
        setContent { MaterialTheme { MyTeamCard(state = state, sendResult = null, onChooseTeam = { chosen += it }) } }
    }

    @Test
    fun `a team that differs only by case is not the chosen one, and choosing it renames the radio`() =
        runComposeUiTest {
            val chosen = mutableListOf<String?>()
            showCard("Drone [Obs]", chosen)

            onNodeWithText("OBS").assertIsNotSelected()
            onNodeWithText("OBS").performClick()

            assertEquals(listOf<String?>("OBS"), chosen)
        }

    @Test
    fun `the team the radio declares exactly stays selected and sends nothing again`() = runComposeUiTest {
        val chosen = mutableListOf<String?>()
        showCard("Drone [OBS]", chosen)

        onNodeWithText("OBS").assertIsSelected()
        onNodeWithText("OBS").performClick()

        assertEquals(emptyList(), chosen)
    }
}
