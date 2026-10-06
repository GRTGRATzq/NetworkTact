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
package org.meshtastic.feature.map.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.proto.Waypoint
import kotlin.test.Test

/** A shared point shows who sent it and when it was received, or says the time is unknown. */
@OptIn(ExperimentalTestApi::class)
class WaypointInfoDialogOriginTest {

    private val waypoint =
        Waypoint.Builder()
            .also { wb ->
                wb.id = 1
                wb.name = "FO 14:05 BRAVO-2"
                wb.description = "fumée"
            }
            .build()

    @Composable
    private fun Dialog(origin: WaypointOrigin) = WaypointInfoDialog(
        waypoint = waypoint,
        displayUnits = MeasurementSystem.METRIC,
        alertsEnabled = false,
        onToggleAlerts = {},
        onDismissRequest = {},
        origin = origin,
    )

    @Test
    fun senderAndReceptionTimeAreShown() = runComposeUiTest {
        setContent { Dialog(WaypointOrigin("BRAVO-2", isMine = false, receivedAtMillis = 1_791_297_000_000L)) }

        onNodeWithText("From BRAVO-2 · received", substring = true).assertIsDisplayed()
    }

    @Test
    fun ownPointAndUnknownTimeSaySo() = runComposeUiTest {
        setContent { Dialog(WaypointOrigin("!0a1a0001", isMine = true, receivedAtMillis = 0L)) }

        onNodeWithText("From me · received time unknown").assertIsDisplayed()
    }
}
