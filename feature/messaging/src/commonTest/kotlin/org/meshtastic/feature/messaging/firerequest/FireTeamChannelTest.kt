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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FireTeamChannelTest {

    @Test
    fun `the confirmed name and its usual spellings are the fire team`() {
        listOf("Équipe TIR", "équipe tir", "EQUIPE TIR", "Equipe Tir", " Équipe  TIR ", "TIR", "tir").forEach {
            assertTrue(SendChannels.isFireTeam(it), it)
        }
    }

    @Test
    fun `a decomposed accent is ignored like a precomposed one`() {
        val decomposed = "Équipe TIR"
        val lowerDecomposed = "équipe tir"

        assertTrue(SendChannels.isFireTeam(decomposed))
        assertTrue(SendChannels.isFireTeam(lowerDecomposed))
    }

    @Test
    fun `dashes are ignored like spaces`() {
        listOf("Équipe-TIR", "Equipe-Tir", "Équipe‑TIR", "Équipe – TIR", "Équipe TIR").forEach {
            assertTrue(SendChannels.isFireTeam(it), it)
        }
    }

    @Test
    fun `other names are not the fire team`() {
        listOf("Équipe Alpha", "Équipe_TIR", "TIRS", "Principal", "", "LongFast").forEach {
            assertFalse(SendChannels.isFireTeam(it), it)
        }
    }

    @Test
    fun `a decomposed fire team channel is chosen by default, otherwise Principal`() {
        val channels = SendChannels.of(listOf("", "Alpha", "Équipe-TIR"))

        assertEquals(ChannelOption(2, "Équipe-TIR"), channels?.fireTeam)
        assertEquals(ChannelOption(0, SendChannels.PRIMARY), SendChannels.of(listOf("", "Alpha"))?.primary)
        assertEquals(null, SendChannels.of(listOf("", "Alpha"))?.fireTeam)
    }
}
