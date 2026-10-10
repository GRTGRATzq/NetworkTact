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
package org.meshtastic.feature.messaging.coordinates

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.model.geo.CoordinateFormat
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CoordinateConverterContentTest {

    @Test
    fun mgrs_input_shows_utm_and_dms() = runComposeUiTest {
        setContent { MaterialTheme { CoordinateConverterContent() } }

        onNode(hasSetTextAction()).performTextInput("31U DQ 48251 11932")

        onNodeWithText("31U 448251 5411932").assertIsDisplayed()
        onNodeWithText("48°51'29\"N 002°17'40\"E").assertIsDisplayed()
    }

    @Test
    fun malformed_mgrs_explains_what_to_fix() = runComposeUiTest {
        setContent { MaterialTheme { CoordinateConverterContent() } }

        onNode(hasSetTextAction()).performTextInput("31U DQ 4825 1193")

        onNodeWithText("MGRS needs exactly 10 digits (5 + 5, 1 m).").assertIsDisplayed()
    }

    @Test
    fun prefilled_input_is_converted_at_once() = runComposeUiTest {
        setContent { MaterialTheme { CoordinateConverterContent(initialInput = "31U DQ 48251 11932") } }

        onNodeWithText("31U 448251 5411932").assertIsDisplayed()
        onNodeWithText("48°51'29\"N 002°17'40\"E").assertIsDisplayed()
    }

    @Test
    fun initial_format_follows_the_prefilled_input() {
        assertEquals(CoordinateFormat.MGRS, initialFormat("31U DQ 48251 11932"))
        assertEquals(CoordinateFormat.UTM, initialFormat("31U 448251 5411932"))
        assertEquals(CoordinateFormat.DMS, initialFormat("48°51'29\"N 002°17'40\"E"))
        assertEquals(CoordinateFormat.MGRS, initialFormat(""))
    }
}
