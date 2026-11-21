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

import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.firereq_intro
import org.meshtastic.core.resources.firereq_renew
import org.meshtastic.core.resources.firereq_target_form_note
import org.meshtastic.core.resources.firereq_target_title
import org.meshtastic.core.resources.getString
import kotlin.test.Test
import kotlin.test.assertTrue

/** Texts that name another control of the screen: they must name it as that control is labelled, in each language. */
class FireRequestTextsTest {

    @Test
    fun `the form note names the Send to selector by its own label`() {
        val title = getString(Res.string.firereq_target_title)
        val note = getString(Res.string.firereq_target_form_note)

        assertTrue(note.contains("« $title »"), "« $title » expected in: $note")
    }

    @Test
    fun `the form intro names the Renew button by its own label`() {
        val renew = getString(Res.string.firereq_renew)
        val intro = getString(Res.string.firereq_intro)

        assertTrue(intro.contains("« $renew »"), "« $renew » expected in: $intro")
    }
}
