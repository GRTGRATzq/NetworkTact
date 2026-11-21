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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** The cards of a request on a channel: broadcast, « Accusé DT », allocation by the fire chief, « GDH fin ». */
@OptIn(ExperimentalTestApi::class)
class FireRequestChannelCardTest {

    private val gdh = "112251 loc NOV 26"
    private val sentDirect = mutableListOf<Pair<String, String>>()
    private val sentParts = mutableListOf<Pair<List<String>, String>>()
    private val actions =
        FireRequestCardActions(
            send = {},
            resend = {},
            showOnMap = null,
            sendParts = { texts, key -> sentParts += texts to key },
            contactKey = "1^all",
            sendDirect = { text, to -> sentDirect += text to to },
            gdhNow = { gdh },
        )

    private fun received(parts: Int = 3, direct: List<Pair<String, org.meshtastic.core.model.Message>> = emptyList()) =
        FireRequestView.of(
            Example.parts.take(parts).mapIndexed { i, text -> Example.message(i + 1L, text, fromLocal = false) },
            direct,
        )

    private val cds = AllocationRadio("!cds", "CDS", "CDS TIR")

    @Test
    fun writersCardSaysBroadcastListsEachAcknowledgementAndResendsTheSheet() = runComposeUiTest {
        val view =
            FireRequestView.of(
                Example.parts.mapIndexed { i, text -> Example.message(i + 1L, text, fromLocal = true) },
                listOf("8!00000002" to Example.message(9, "[DT AB1234] Accusé DT $gdh", fromLocal = false)),
            )

        setContent {
            AppTheme { FireRequestCard(view.assembly.threads.single(), view, null, true, actions, channelName = "TIR") }
        }

        onNodeWithText("Broadcast on TIR", substring = true).assertExists()
        onNodeWithText("DT acknowledgement:", substring = true).assertExists()
        onNodeWithText("Resend the sheet").performClick()
        assertEquals(listOf(Example.parts to "1^all"), sentParts)
    }

    @Test
    fun dtAcknowledgementWaitsForTheWholeSheetThenGoesToTheWriterOnly() = runComposeUiTest {
        val partial = received(parts = 2)
        setContent {
            AppTheme {
                FireRequestCard(partial.assembly.threads.single(), partial, null, true, actions, channelName = "TIR")
            }
        }
        onNodeWithText("DT acknowledgement").assertIsNotEnabled()
        onNodeWithText("Available once the sheet is complete (2 / 3).").assertExists()
    }

    @Test
    fun completeSheetSendsOneDtAcknowledgementInADirectMessage() = runComposeUiTest {
        val view = received()
        setContent {
            AppTheme { FireRequestCard(view.assembly.threads.single(), view, null, true, actions, channelName = "TIR") }
        }
        onNodeWithText("DT acknowledgement").assertIsEnabled().performClick()
        assertEquals(listOf("[DT AB1234] Accusé DT $gdh" to "!00000002"), sentDirect)
    }

    @Test
    fun aDtAcknowledgementAlreadySentStaysGreyed() = runComposeUiTest {
        val own = Example.message(9, "[DT AB1234] Accusé DT $gdh", fromLocal = true)
        val view = received(direct = listOf("8!00000002" to own))
        setContent {
            AppTheme { FireRequestCard(view.assembly.threads.single(), view, null, true, actions, channelName = "TIR") }
        }
        onNodeWithText("Acknowledgement sent $gdh").assertIsNotEnabled()
    }

    @Test
    fun onlyTheFireChiefSeesTheAllocationAndMustChooseARadioFirst() = runComposeUiTest {
        val view = received()
        val chief = FireRequestNetwork(FireChiefRole.CHIEF, listOf(cds), ourId = "!chief", ourShortName = "CT")
        setContent {
            AppTheme {
                FireRequestCard(
                    view.assembly.threads.single(),
                    view,
                    null,
                    true,
                    actions,
                    channelName = "TIR",
                    network = chief,
                )
            }
        }
        onNodeWithText("Fire allocated to:").assertExists()
        onNodeWithText("Allocate").assertIsNotEnabled()
        // Which messages « Attribuer » sends: FireRequestAllocationTest (the menu does not open in desktop tests).
        onNodeWithText("Choose a radio").assertIsEnabled()
    }

    @Test
    fun afterAnAllocationTheChiefAlwaysSeesResendToTheRadioWithANoteThatSaysWhy() = runComposeUiTest {
        val made = Example.message(9, "[DT AB1234] Tir attribué à vous par OA · GDH attribution $gdh", fromLocal = true)
        val view = received(direct = listOf("8!cds" to made))
        val chief = FireRequestNetwork(FireChiefRole.CHIEF, listOf(cds), ourId = "!chief", ourShortName = "CT")
        setContent {
            AppTheme {
                FireRequestCard(
                    view.assembly.threads.single(),
                    view,
                    null,
                    true,
                    actions,
                    channelName = "TIR",
                    network = chief,
                )
            }
        }
        onNodeWithText("Allocated to CDS · CDS TIR, $gdh").assertExists()
        onNodeWithText("Resend the sheet to CDS").assertExists()
        onNodeWithText("Shown after every allocation", substring = true).assertExists()
        onNodeWithText("not on this channel.", substring = true).assertDoesNotExist()
    }

    @Test
    fun withoutAChiefAllocationIsNotOfferedAndTheGdhButtonsOfV1AreGone() = runComposeUiTest {
        val view = received()
        setContent {
            AppTheme { FireRequestCard(view.assembly.threads.single(), view, null, true, actions, channelName = "TIR") }
        }
        onNodeWithText("No fire chief in the organisation chart.").assertExists()
        onNodeWithText("Allocate").assertDoesNotExist()
        onAllNodes(hasText("GDH attribution") and hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun theDesignatedRadioSendsGdhFin() = runComposeUiTest {
        val designation =
            Example.message(9, "[DT AB1234] Tir attribué à vous par OA · GDH attribution $gdh", fromLocal = false)
        val view = received(direct = listOf("8!00000002" to designation))
        // The sheet and the allocation both come from !00000002, the writer named « OA »: GDH fin goes back to it.
        val other = FireRequestNetwork(FireChiefRole.OTHER, listOf(cds, AllocationRadio("!00000002", "OA", "OA")))
        setContent {
            AppTheme {
                FireRequestCard(
                    view.assembly.threads.single(),
                    view,
                    null,
                    true,
                    actions,
                    channelName = "TIR",
                    network = other,
                )
            }
        }
        onNodeWithText("Fire allocated to you", substring = true).assertExists()
        onAllNodes(hasText("GDH fin") and hasClickAction()).assertCountEquals(1)
        onNode(hasText("GDH fin") and hasClickAction()).assertIsEnabled()
        onNodeWithText("Allocate").assertDoesNotExist()
    }

    @Test
    fun gdhFinIsGreyedWithItsReasonWhenTheSheetsSenderIsUnknown() = runComposeUiTest {
        val designation =
            Example.message(9, "[DT AB1234] Tir attribué à vous par OA · GDH attribution $gdh", fromLocal = false)
        val view = received(direct = listOf("8!00000002" to designation))
        val other = FireRequestNetwork(FireChiefRole.OTHER, listOf(cds))
        setContent {
            AppTheme {
                FireRequestCard(
                    view.assembly.threads.single(),
                    view,
                    null,
                    true,
                    actions,
                    channelName = "TIR",
                    network = other,
                )
            }
        }
        onNode(hasText("GDH fin") and hasClickAction()).assertIsNotEnabled()
        onNodeWithText("is not among the radios of the network", substring = true).assertExists()
    }
}
