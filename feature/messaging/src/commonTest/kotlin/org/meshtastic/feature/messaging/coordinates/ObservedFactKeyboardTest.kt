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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/**
 * D10: in « Fait observé », the keyboard's action key (✓, « OK ») closes the keyboard in the time and the description,
 * as it already did in the place. The form is a dialog, a window of its own: the test goes through the real dialog.
 */
@OptIn(ExperimentalTestApi::class)
class ObservedFactKeyboardTest {

    /** The text fields of the form, in their order: time, place, description. */
    private fun ComposeUiTest.field(index: Int): SemanticsNodeInteraction = onAllNodes(hasSetTextAction())[index]

    private fun ComposeUiTest.pressActionKeyIn(index: Int) {
        setContent { MaterialTheme { ObservedFactDialog(onInsert = {}, onDismiss = {}) } }
        field(index).performClick()
        field(index).assertIsFocused()

        field(index).performImeAction()

        field(index).assertIsNotFocused()
    }

    @Test fun `the action key leaves the time field`() = runComposeUiTest { pressActionKeyIn(TIME) }

    @Test fun `the action key leaves the description field`() = runComposeUiTest { pressActionKeyIn(DESCRIPTION) }

    private companion object {
        const val TIME = 0
        const val DESCRIPTION = 2
    }
}
