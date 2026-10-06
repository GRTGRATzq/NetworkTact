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
package org.meshtastic.feature.messaging.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.component.preview.NodePreviewParameterProvider
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.MapFocusPoint
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.Routing
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class MessageItemTest {

    @Test
    fun mqttIconIsDisplayedWhenViaMqttIsTrue() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().minnieMouse
        val messageWithMqtt =
            Message(
                text = "Test message via MQTT",
                time = "10:00",
                fromLocal = false,
                status = MessageStatus.RECEIVED,
                snr = 2.5f,
                rssi = 90,
                hopsAway = 0,
                uuid = 1L,
                receivedTime = nowMillis,
                node = testNode,
                read = false,
                routingError = 0,
                packetId = 1234,
                emojis = listOf(),
                replyId = null,
                viaMqtt = true,
            )

        setContent {
            MessageItem(
                message = messageWithMqtt,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        // Check that the MQTT icon is displayed
        onNodeWithContentDescription("MQTT").assertIsDisplayed()
    }

    @Test
    fun directMessageWithoutSnrDoesNotFabricateAZeroReading() = runComposeUiTest {
        // Before DataPacket/Message.snr became nullable, an absent SNR narrowed to 0f on the way through the mapper
        // and this row rendered "SNR 0.00 dB" — a measurement the radio never took.
        val testNode = NodePreviewParameterProvider().minnieMouse
        val message = directMessage(node = testNode, snr = null)

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        onNodeWithText("SNR 0.00 dB", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun directMessageWithZeroSnrShowsTheReading() = runComposeUiTest {
        // The other half: 0 dB is a real, strong reading and must still render.
        val testNode = NodePreviewParameterProvider().minnieMouse
        val message = directMessage(node = testNode, snr = 0f)

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        onNodeWithText("SNR 0.00 dB", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun senderTeamIsShownAfterTheNameWithoutTheSuffix() = runComposeUiTest {
        val base = NodePreviewParameterProvider().minnieMouse
        val sender = base.copy(user = base.user.newBuilder().also { it.long_name = "ALPHA-1 [Alpha]" }.build())
        val message = directMessage(node = sender, snr = 1f)

        setContent {
            AppTheme {
                MessageItem(
                    message = message,
                    node = sender,
                    selected = false,
                    onClick = {},
                    onLongClick = {},
                    onStatusClick = {},
                    ourNode = base,
                )
            }
        }

        onNodeWithText("ALPHA-1 · Team Alpha", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("ALPHA-1 [Alpha]", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun directMessage(node: Node, snr: Float?) = Message(
        text = "Direct message",
        time = "10:00",
        fromLocal = false,
        status = MessageStatus.RECEIVED,
        snr = snr,
        rssi = -90,
        hopsAway = 0,
        uuid = 1L,
        receivedTime = nowMillis,
        node = node,
        read = false,
        routingError = 0,
        packetId = 1234,
        emojis = listOf(),
        replyId = null,
        viaMqtt = false,
    )

    @Test
    fun mqttIconIsNotDisplayedWhenViaMqttIsFalse() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().minnieMouse
        val messageWithoutMqtt =
            Message(
                text = "Test message not via MQTT",
                time = "10:00",
                fromLocal = false,
                status = MessageStatus.RECEIVED,
                snr = 2.5f,
                rssi = 90,
                hopsAway = 0,
                uuid = 1L,
                receivedTime = nowMillis,
                node = testNode,
                read = false,
                routingError = 0,
                packetId = 1234,
                emojis = listOf(),
                replyId = null,
                viaMqtt = false,
            )

        setContent {
            MessageItem(
                message = messageWithoutMqtt,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        // Check that the MQTT icon is not displayed
        onNodeWithContentDescription("MQTT").assertDoesNotExist()
    }

    @Test
    fun messageItem_hasCorrectSemanticContentDescription() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().minnieMouse
        val message =
            Message(
                text = "Hello World",
                time = "10:00",
                fromLocal = false,
                status = MessageStatus.RECEIVED,
                snr = 2.5f,
                rssi = 90,
                hopsAway = 0,
                uuid = 1L,
                receivedTime = nowMillis,
                node = testNode,
                read = false,
                routingError = 0,
                packetId = 1234,
                emojis = listOf(),
                replyId = null,
                viaMqtt = false,
            )

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onClick = {},
                onLongClick = {},
                onStatusClick = {},
                ourNode = testNode,
            )
        }

        // Verify that the node containing the message text exists and matches the text
        onNodeWithContentDescription("Message from ${testNode.user.long_name}: Hello World").assertIsDisplayed()
    }

    @Test
    fun localDirectMessage_ackFromRecipientNamesTheRecipient() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message = localMessage(node = testNode, status = MessageStatus.RECEIVED)

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onStatusClick = {},
                ourNode = testNode,
                isDirectMessage = true,
                recipientName = "BRAVO-2",
            )
        }

        onNodeWithText("Acknowledged by BRAVO-2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun localDirectMessage_validProofReadsVerified() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message =
            localMessage(node = testNode, status = MessageStatus.RECEIVED)
                .copy(ackProofStatus = MeshPacket.AckProofStatus.ACK_PROOF_VALID.value)

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onStatusClick = {},
                ourNode = testNode,
                isDirectMessage = true,
                recipientName = "BRAVO-2",
            )
        }

        onNodeWithText("Acknowledged by BRAVO-2 (verified)", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun localDirectMessage_invalidProofIsAnAlertNeverAnAck() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message =
            localMessage(node = testNode, status = MessageStatus.RECEIVED)
                .copy(ackProofStatus = MeshPacket.AckProofStatus.ACK_PROOF_INVALID.value)

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onStatusClick = {},
                ourNode = testNode,
                isDirectMessage = true,
                recipientName = "BRAVO-2",
            )
        }

        onNodeWithText("WARNING: invalid delivery proof, possible forgery", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Acknowledged by BRAVO-2", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(MESSAGE_STATUS_LABEL_TEST_TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SentStatusAlertKey, true))
    }

    @Test
    fun localChannelMessage_markedDelivered_readsRelayedNeverAcknowledged() = runComposeUiTest {
        assertChannelStatusReadsRelayed(MessageStatus.DELIVERED)
    }

    @Test
    fun localChannelMessage_markedReceived_stillReadsRelayed() = runComposeUiTest {
        assertChannelStatusReadsRelayed(MessageStatus.RECEIVED)
    }

    private fun ComposeUiTest.assertChannelStatusReadsRelayed(status: MessageStatus) {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message = localMessage(node = testNode, status = status)
        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onStatusClick = {},
                ourNode = testNode,
                isDirectMessage = false,
                recipientName = "PC-0",
            )
        }

        onNodeWithText("Relayed by the mesh", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Acknowledged", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun urgentPrefix_headsTheCardAndIsNotRepeatedInTheBody() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message = localMessage(node = testNode, status = MessageStatus.ENROUTE).copy(text = "[URG] ALPHA-1 appui")

        setContent {
            MessageItem(message = message, node = testNode, selected = false, onStatusClick = {}, ourNode = testNode)
        }

        onNodeWithText("[URG]", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithContentDescription("Priority: Urgent", useUnmergedTree = true).assertExists()
        onNodeWithText("ALPHA-1 appui", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("[URG] ALPHA-1 appui", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(PRIORITY_BAR_TEST_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun receivedChannelCard_namesTheSenderTeamAndTheChannel() = runComposeUiTest {
        val sender = namedNode(NodePreviewParameterProvider().minnieMouse, "ALPHA-1 [Alpha]")
        val message = directMessage(node = sender, snr = 1f).copy(text = "[CR] FAIT OBSERVÉ · Lieu 31U DQ 48251 11932")

        setContent {
            AppTheme {
                MessageItem(
                    message = message,
                    node = sender,
                    selected = false,
                    ourNode = NodePreviewParameterProvider().mickeyMouse,
                    isDirectMessage = false,
                    conversationName = "Général",
                )
            }
        }

        onNodeWithText("[CR]", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("ALPHA-1 · Team Alpha", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Général", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("FAIT OBSERVÉ · Lieu 31U DQ 48251 11932", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun receivedDirectCard_readsDirectOnTheEndSide() = runComposeUiTest {
        val sender = namedNode(NodePreviewParameterProvider().minnieMouse, "BRAVO-2 [Bravo]")
        val message = directMessage(node = sender, snr = 1f).copy(text = "[POS] BRAVO-2 · MGRS 31U DQ 48251 11932")

        setContent {
            MessageItem(
                message = message,
                node = sender,
                selected = false,
                ourNode = NodePreviewParameterProvider().mickeyMouse,
                isDirectMessage = true,
            )
        }

        onNodeWithText("Direct", useUnmergedTree = true).assertIsDisplayed()
        // [POS] is information: no priority tag, and its own tag stays in the body.
        onNodeWithContentDescription("Priority:", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("[POS] BRAVO-2 · MGRS 31U DQ 48251 11932", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun receivedCoordinateOffersToShowItOnTheMap() = runComposeUiTest {
        val sender = namedNode(NodePreviewParameterProvider().minnieMouse, "BRAVO-2 [Bravo]")
        val message = directMessage(node = sender, snr = 1f).copy(text = "[POS] BRAVO-2 · MGRS 31U DQ 48251 11932")
        var shown: MapFocusPoint? = null

        setContent {
            MessageItem(
                message = message,
                node = sender,
                selected = false,
                ourNode = NodePreviewParameterProvider().mickeyMouse,
                onShowOnMap = { shown = it },
            )
        }

        onNodeWithText("Show on map", useUnmergedTree = true).assertIsDisplayed().performClick()
        val point = checkNotNull(shown)
        // 31U DQ 48251 11932 is 48.858 19° N, 2.294 49° E.
        assertTrue(abs(point.latitude - 48.858_19) < 0.000_1 && abs(point.longitude - 2.294_49) < 0.000_1, "$point")
    }

    @Test
    fun receivedTextWithoutCoordinateHasNoMapButton() = runComposeUiTest {
        val sender = namedNode(NodePreviewParameterProvider().minnieMouse, "BRAVO-2 [Bravo]")
        val message = directMessage(node = sender, snr = 1f).copy(text = "Appelez le 06 12 34 56 78 à 14:05")

        setContent {
            MessageItem(
                message = message,
                node = sender,
                selected = false,
                ourNode = NodePreviewParameterProvider().mickeyMouse,
                onShowOnMap = {},
            )
        }

        onNodeWithText("Show on map", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun sentDirectCard_isHeadedMeToTheRecipient() = runComposeUiTest {
        val me = namedNode(NodePreviewParameterProvider().mickeyMouse, "PC-0")
        val message = localMessage(node = me, status = MessageStatus.ENROUTE)

        setContent {
            MessageItem(
                message = message,
                node = me,
                selected = false,
                ourNode = me,
                isDirectMessage = true,
                recipientName = "BRAVO-2",
            )
        }

        onNodeWithText("Me → BRAVO-2 · Direct", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun sentChannelCard_isHeadedMeToTheChannel() = runComposeUiTest {
        val me = namedNode(NodePreviewParameterProvider().mickeyMouse, "PC-0")
        val message = localMessage(node = me, status = MessageStatus.ENROUTE).copy(text = "[URG] Regroupement")

        setContent {
            MessageItem(
                message = message,
                node = me,
                selected = false,
                ourNode = me,
                isDirectMessage = false,
                conversationName = "Général",
            )
        }

        onNodeWithText("[URG]", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Me → Général", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun receivedCard_readsReceivedWithItsAge() = runComposeUiTest {
        val sender = namedNode(NodePreviewParameterProvider().minnieMouse, "ALPHA-1 [Alpha]")
        val message = directMessage(node = sender, snr = 1f)

        setContent {
            MessageItem(
                message = message,
                node = sender,
                selected = false,
                ourNode = NodePreviewParameterProvider().mickeyMouse,
                currentTimeMillis = { message.displayTime + 2.minutes.inWholeMilliseconds },
            )
        }

        onNodeWithText("Received · 2 min ago", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun receivedCard_ageFollowsTheClockWithoutANewMessage() = runComposeUiTest {
        val sender = namedNode(NodePreviewParameterProvider().minnieMouse, "ALPHA-1 [Alpha]")
        val message = directMessage(node = sender, snr = 1f)
        var now by mutableStateOf(message.displayTime)

        setContent {
            MessageItem(
                message = message,
                node = sender,
                selected = false,
                ourNode = NodePreviewParameterProvider().mickeyMouse,
                currentTimeMillis = { now },
            )
        }
        onNodeWithText("Received · just now", useUnmergedTree = true).assertIsDisplayed()

        now = message.displayTime + 5.minutes.inWholeMilliseconds
        waitForIdle()

        onNodeWithText("Received · 5 min ago", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Received · just now", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun sentCard_hasNoReceivedLine() = runComposeUiTest {
        val me = namedNode(NodePreviewParameterProvider().mickeyMouse, "PC-0")
        val message = localMessage(node = me, status = MessageStatus.ENROUTE)

        setContent { MessageItem(message = message, node = me, selected = false, ourNode = me) }

        onNodeWithTag(MESSAGE_RECEIVED_LABEL_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Sent to my radio", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun namedNode(base: Node, longName: String): Node =
        base.copy(user = base.user.newBuilder().also { it.long_name = longName }.build())

    @Test
    fun localDirectMessage_displaysImplicitAckWarningText() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message = localMessage(node = testNode, status = MessageStatus.DELIVERED)

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onStatusClick = {},
                ourNode = testNode,
                isDirectMessage = true,
            )
        }

        onNodeWithText("Relayed by the mesh", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun localMessage_displaysRoutingErrorStatusText() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message =
            localMessage(
                node = testNode,
                status = MessageStatus.ERROR,
                routingError = Routing.Error.MAX_RETRANSMIT.value,
            )

        setContent {
            MessageItem(message = message, node = testNode, selected = false, onStatusClick = {}, ourNode = testNode)
        }

        onNodeWithText("Failed: no node confirmed receipt", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun channelKeyMismatch_displaysTerminalStatusText() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message =
            localMessage(node = testNode, status = MessageStatus.ERROR, routingError = Routing.Error.NO_CHANNEL.value)

        setContent {
            MessageItem(message = message, node = testNode, selected = false, onStatusClick = {}, ourNode = testNode)
        }

        onNodeWithText("Failed: channel or key mismatch", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun routingError_isDrawnAsAnAlert() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message =
            localMessage(
                node = testNode,
                status = MessageStatus.ERROR,
                routingError = Routing.Error.MAX_RETRANSMIT.value,
            )

        setContent {
            AppTheme {
                MessageItem(
                    message = message,
                    node = testNode,
                    selected = false,
                    onStatusClick = {},
                    ourNode = testNode,
                )
            }
        }

        onNodeWithTag(MESSAGE_STATUS_LABEL_TEST_TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SentStatusAlertKey, true))
        onNodeWithContentDescription("Alert", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun messageStatusDialog_displaysRoutingFailureExplanation() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message =
            localMessage(
                node = testNode,
                status = MessageStatus.ERROR,
                routingError = Routing.Error.MAX_RETRANSMIT.value,
            )

        setContent { MessageStatusDialog(message = message, resendOption = true, onResend = {}, onDismiss = {}) }

        onNodeWithText("Failed: no node confirmed receipt").assertIsDisplayed()
        onNodeWithText("No node confirmed this message. Try again when you have better signal or more mesh coverage.")
            .assertIsDisplayed()
        onNodeWithText("Resend").assertIsDisplayed()
    }

    @Test
    fun localMessageStatus_invokesStatusClick() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message = localMessage(node = testNode, status = MessageStatus.QUEUED)
        var statusClicks = 0

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onStatusClick = { statusClicks += 1 },
                ourNode = testNode,
            )
        }

        onNodeWithText("Waiting for my radio", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag(MESSAGE_STATUS_LABEL_TEST_TAG, useUnmergedTree = true).performClick()

        assertEquals(1, statusClicks)
    }

    @Test
    fun localMessageStatus_doesNotExposeGenericIconDescription() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val message = localMessage(node = testNode, status = MessageStatus.ENROUTE)

        setContent {
            MessageItem(message = message, node = testNode, selected = false, onStatusClick = {}, ourNode = testNode)
        }

        onNodeWithText("Sent to my radio", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithContentDescription("Message delivery status", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun fullMessageTimestampIsDisplayedInMessageHeader() = runComposeUiTest {
        val testNode = NodePreviewParameterProvider().mickeyMouse
        val meshTime = 1_700_000_000_000L
        val message =
            localMessage(node = testNode, status = MessageStatus.RECEIVED).copy(time = "compact", meshTime = meshTime)
        val expectedTimestamp = DateFormatter.formatDateTime(meshTime)

        setContent {
            MessageItem(
                message = message,
                node = testNode,
                selected = false,
                onStatusClick = {},
                ourNode = testNode,
                showFullMessageTimestamp = true,
            )
        }

        onNodeWithText(expectedTimestamp, useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("compact", useUnmergedTree = true).assertDoesNotExist()
    }

    private fun localMessage(node: Node, status: MessageStatus, routingError: Int = 0) = Message(
        text = "Local message",
        time = "10:00",
        fromLocal = true,
        status = status,
        snr = 2.5f,
        rssi = 90,
        hopsAway = 0,
        uuid = 1L,
        receivedTime = nowMillis,
        node = node,
        read = false,
        routingError = routingError,
        packetId = 1234,
        emojis = listOf(),
        replyId = null,
        viaMqtt = false,
    )
}
