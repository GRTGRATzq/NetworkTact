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
package org.meshtastic.feature.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/** A3: the Settings sections open and close from their title; only the first one is open at first. */
@OptIn(ExperimentalTestApi::class)
class ExpressiveSectionCollapseTest {

    @Test
    fun `a closed section shows its title only, and opens and closes from it`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Column {
                    ExpressiveSection(title = "Configuration", collapsible = true) { Text("LoRa") }
                    ExpressiveSection(title = "App", collapsible = true, initiallyExpanded = false) { Text("Thème") }
                }
            }
        }

        onNodeWithText("LoRa").assertIsDisplayed()
        onNodeWithText("App").assertIsDisplayed()
        onNodeWithText("Thème").assertDoesNotExist()

        onNodeWithText("App").performClick()
        onNodeWithText("Thème").assertIsDisplayed()

        onNodeWithText("App").performClick()
        onNodeWithText("Thème").assertDoesNotExist()
        onNodeWithText("LoRa").assertIsDisplayed()
    }

    @Test
    fun `a section that is not collapsible always shows its content`() = runComposeUiTest {
        setContent { MaterialTheme { ExpressiveSection(title = "Applications") { Text("Remerciements") } } }

        onNodeWithText("Remerciements").assertIsDisplayed()
        onNodeWithText("Applications").performClick()
        onNodeWithText("Remerciements").assertIsDisplayed()
    }
}
