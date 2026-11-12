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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.firerequest.AdjustMethod
import org.meshtastic.core.model.firerequest.Adjustment
import org.meshtastic.core.model.firerequest.Charge
import org.meshtastic.core.model.firerequest.Command
import org.meshtastic.core.model.firerequest.Cover
import org.meshtastic.core.model.firerequest.Effect
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.FireType
import org.meshtastic.core.model.firerequest.Friends
import org.meshtastic.core.model.firerequest.ImpactLine
import org.meshtastic.core.model.firerequest.Location
import org.meshtastic.core.model.firerequest.LruLine
import org.meshtastic.core.model.firerequest.Shape
import org.meshtastic.core.model.firerequest.ShapeKind
import org.meshtastic.core.model.firerequest.Targets
import org.meshtastic.core.testing.TestDataFactory
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.proto.Routing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The validated example of `docs/SPEC-DEMANDE-DE-TIR.md` § 4, and messages carrying it. */
private object Example {
    val request =
        FireRequest(
            origin = "OA",
            recipient = "CDS TIR",
            requestGdh = "112250 loc NOV 26",
            number = "AB1234",
            fireType = FireType(lines = setOf(ImpactLine.G), firstRound = true, prepare = true),
            effect = Effect(neutralisation = true, mixFirst = "1", mixSecond = "2"),
            charge = Charge(launcher = "2", trajectoryFirst = "1", modeFirst = "1"),
            targets = Targets(personnel = true, personnelCover = Cover.OPEN),
            location = Location(g = LruLine("35", "T", "LL", "33000", "90300", "610"), tle = "2"),
            shape = Shape(kind = ShapeKind.CIRCULAR, radius = "100"),
            adjustment =
            Adjustment(
                method = AdjustMethod.GRID,
                observationBearing = "4800",
                observerX = "34550",
                observerY = "88800",
                observerZ = "640",
            ),
            friends = Friends(distance = "800", bearing = "1600", protection = "1"),
            command = Command(whenReady = true),
        )

    val parts = FireRequestPreview.of(request).parts

    private val node = TestDataFactory.createTestNode(num = 2, userId = "!00000002", longName = "OA")

    fun message(
        uuid: Long,
        text: String,
        fromLocal: Boolean,
        status: MessageStatus = MessageStatus.RECEIVED,
        routingError: Int = 0,
    ) = Message(
        uuid = uuid,
        receivedTime = 1_000_000L + uuid * 60_000L,
        node = node,
        text = text,
        fromLocal = fromLocal,
        time = "22:50",
        read = true,
        status = status,
        routingError = routingError,
        packetId = uuid.toInt(),
        emojis = emptyList(),
        snr = null,
        rssi = null,
        hopsAway = 0,
        replyId = null,
    )
}

/** Which messages a request hides from the list, and where it is shown. */
class FireRequestViewTest {

    @Test
    fun `the parts of a request are shown once, at the latest, ordinary messages left alone`() {
        val messages =
            listOf(
                Example.message(1, "Reçu, je prépare une demande.", fromLocal = false),
                Example.message(2, Example.parts[0], fromLocal = false),
                Example.message(3, Example.parts[2], fromLocal = false),
                Example.message(4, "texte [DT AB1234 2/3]", fromLocal = false),
                Example.message(5, Example.parts[1], fromLocal = false),
            )

        val view = FireRequestView.of(messages)

        assertEquals(setOf(2L, 3L), view.hiddenIds)
        val thread = assertNotNull(view.assembly.threadAt(5))
        assertTrue(thread.isComplete)
        assertNull(view.assembly.threadAt(1))
        assertNull(view.assembly.threadAt(4))
        assertEquals(setOf(2L, 3L, 5L), view.messages.keys)
    }

    @Test
    fun `a conversation without request hides nothing`() {
        val view = FireRequestView.of(listOf(Example.message(1, "Bien reçu", fromLocal = false)))

        assertEquals(FireRequestView.EMPTY, view)
        assertTrue(view.hiddenIds.isEmpty())
    }
}

/** The cards shown in the conversation: the sender's and the recipient's (`docs/maquette-demande-de-tir.pdf`). */
@OptIn(ExperimentalTestApi::class)
class FireRequestCardTest {

    private val noActions = FireRequestCardActions(send = {}, resend = {}, showOnMap = null)

    @Test
    fun receivedSheetCountsThePartsAndNamesTheMissingOne() = runComposeUiTest {
        val view =
            FireRequestView.of(
                listOf(
                    Example.message(1, Example.parts[0], fromLocal = false),
                    Example.message(2, Example.parts[1], fromLocal = false),
                ),
            )
        val thread = view.assembly.threads.single()

        setContent { AppTheme { FireRequestCard(thread, view, null, canSend = true, actions = noActions) } }

        onNodeWithText("Fire request AB1234").assertExists()
        onNodeWithText("2 / 3 received").assertExists()
        onNodeWithText("Missing part(s) 3", substring = true).assertExists()
        onAllNodesWithText("not received", substring = true).fetchSemanticsNodes().isNotEmpty().let(::assertTrue)
        // Box 4 was left empty on the sheet: « non rempli », a word of the sheet, the same in every language.
        onNodeWithText("non rempli", substring = true).assertExists()
        onNodeWithText("Mo 120", substring = true).assertExists()
        onNodeWithText("Show the 2 raw messages").assertExists()
    }

    @Test
    fun sendersCardShowsEachPartAndResendsTheFailedOne() = runComposeUiTest {
        val failed =
            Example.message(
                2,
                Example.parts[1],
                fromLocal = true,
                status = MessageStatus.ERROR,
                routingError = Routing.Error.MAX_RETRANSMIT.value,
            )
        val view =
            FireRequestView.of(
                listOf(
                    Example.message(1, Example.parts[0], fromLocal = true),
                    failed,
                    Example.message(3, Example.parts[2], fromLocal = true),
                    Example.message(4, "[DT AB1234] GDH attribution 112252 loc NOV 26", fromLocal = false),
                ),
            )
        val thread = view.assembly.threads.single()
        val resent = mutableListOf<Message>()
        val actions = FireRequestCardActions(send = {}, resend = { resent += it }, showOnMap = null)

        setContent { AppTheme { FireRequestCard(thread, view, "CDS TIR", canSend = true, actions = actions) } }

        onNodeWithText("DT AB1234 · 3 messages").assertExists()
        onNodeWithText("GDH attribution received: 112252 loc NOV 26").assertExists()
        onNodeWithText("GDH fin: waiting").assertExists()
        onAllNodesWithText("Resend part", substring = true).assertCountEquals(1)
        onNodeWithText("Resend part 2").performClick()
        assertEquals(listOf(failed), resent)
    }

    @Test
    fun aFailedPartCannotBeResentWhileTheRadioIsAway() = runComposeUiTest {
        val view =
            FireRequestView.of(
                listOf(
                    Example.message(
                        1,
                        Example.parts[0],
                        fromLocal = true,
                        status = MessageStatus.ERROR,
                        routingError = Routing.Error.MAX_RETRANSMIT.value,
                    ),
                ),
            )

        setContent {
            AppTheme {
                FireRequestCard(view.assembly.threads.single(), view, "CDS TIR", canSend = false, actions = noActions)
            }
        }

        onAllNodesWithText("Resend part", substring = true).assertCountEquals(0)
    }

    @Test
    fun sizeBarShowsBytesAndPartsAndWhySendingIsGreyed() = runComposeUiTest {
        val preview = FireRequestPreview.of(Example.request)

        setContent { AppTheme { FireRequestSizeBar(preview, SendBlock.DISCONNECTED) } }

        onNodeWithText("488 bytes · 3 messages").assertExists()
        onNodeWithText("Radio not connected: cannot send.").assertExists()
    }

    @Test
    fun sizeBarWithoutBlockShowsNoReason() = runComposeUiTest {
        val preview = FireRequestPreview.of(Example.request.copy(number = ""))

        setContent { AppTheme { FireRequestSizeBar(preview, SendBlock.NO_NUMBER) } }

        onNodeWithText("Type the fire number to send.").assertExists()
    }
}
