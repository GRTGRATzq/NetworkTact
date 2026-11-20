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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getString
import org.meshtastic.core.resources.save
import kotlin.test.Test
import kotlin.test.assertEquals

/** A4: renaming a map of « Cartes sur le téléphone ». */
@OptIn(ExperimentalTestApi::class)
class RenameDialogTest {

    private val map =
        OfflineMapFile(id = "a1", name = "Carte 45,9N 24,8E z14", sizeBytes = 1, minZoom = 0, maxZoom = 14)

    @Test
    fun `the new name, tidied, is given once saved`() = runComposeUiTest {
        val renamed = mutableListOf<String>()
        setContent { MaterialTheme { RenameDialog(map = map, onConfirm = { renamed += it }, onDismiss = {}) } }
        val save = getString(Res.string.save)

        // Unchanged: nothing to save.
        onNodeWithText(save).assertIsNotEnabled()

        onNode(hasSetTextAction()).performTextClearance()
        onNodeWithText(save).assertIsNotEnabled()

        onNode(hasSetTextAction()).performTextInput("  Cincu   nord ")
        onNodeWithText(save).assertIsEnabled()
        onNodeWithText(save).performClick()

        assertEquals(listOf("Cincu nord"), renamed)
    }
}
