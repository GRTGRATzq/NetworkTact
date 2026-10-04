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
package org.meshtastic.core.demo.data

import kotlinx.datetime.TimeZone
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.MyNodeInfo
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.freshness.PositionState
import org.meshtastic.core.model.geo.CoordinateFormat
import org.meshtastic.core.model.geo.FormattedCoordinates
import org.meshtastic.core.model.geo.LatLon
import org.meshtastic.core.model.geo.ObservedFactMessage
import org.meshtastic.core.model.geo.PositionMessage
import org.meshtastic.core.model.geo.PositionSource
import org.meshtastic.core.model.team.TeamRoster
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.proto.ChannelSet
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.Position
import org.meshtastic.proto.Routing
import org.meshtastic.proto.User
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** One stored message of the demo history, in the shape the real database keeps it. */
data class DemoPacket(
    val uuid: Long,
    val contactKey: String,
    val packet: DataPacket,
    val receivedTime: Long,
    val read: Boolean,
    val hopsAway: Int,
    val routingError: Int = 0,
)

/** Everything demo mode shows. [nodes] includes this phone's own node, [myNodeInfo]'s. */
data class DemoData(
    val myNodeInfo: MyNodeInfo,
    val nodes: List<Node>,
    val channelSet: ChannelSet,
    val roster: TeamRosterRecord,
    val packets: List<DemoPacket>,
)

/**
 * The fictitious data set, built afresh at each activation so its ages read the same whenever the demo starts. It
 * stages every NetworkTact function: teams in the node names and a team list, positions fresh, old and without a fix
 * time for the command post view, info / report / urgent messages, a `[POS]` and a `[CR] FAIT OBSERVÉ`, three channels,
 * and sent messages in every state.
 */
object DemoDataSet {
    const val PC0_NUM = 0x0dec0000
    const val ALPHA1_NUM = 0x0dec0001
    const val BRAVO2_NUM = 0x0dec0002
    const val CHARLIE3_NUM = 0x0dec0003

    const val GENERAL = "Général"
    const val TEAM_ALPHA = "Équipe Alpha"
    const val COMMAND_POST = "PC"

    /** Channel conversations, by channel index. */
    const val GENERAL_KEY = "0${NodeAddress.ID_BROADCAST}"
    const val TEAM_ALPHA_KEY = "1${NodeAddress.ID_BROADCAST}"
    const val COMMAND_POST_KEY = "2${NodeAddress.ID_BROADCAST}"

    /** Direct conversations, on the public-key channel the app uses for them. */
    val BRAVO2_DIRECT_KEY = "${NodeAddress.PKC_CHANNEL_INDEX}${id(BRAVO2_NUM)}"
    val CHARLIE3_DIRECT_KEY = "${NodeAddress.PKC_CHANNEL_INDEX}${id(CHARLIE3_NUM)}"

    val TEAMS = listOf("Alpha", "Bravo")

    private const val COORDINATE_SCALE = 1e7
    private const val MILLIS_PER_SECOND = 1_000L
    private const val MAX_CHANNELS = 8
    private const val FIRST_PACKET_ID = 0x0dec_1000

    private val alpha1Point = LatLon(48.856_667, 2.350_833)
    private val observedPoint = LatLon(48.854_200, 2.356_400)
    private val alpha1PositionAge = 2.minutes
    private val rosterAge = 30.minutes

    /** The nodes: PC-0 is this phone's own; CHARLIE-3 has a position stored without any time. */
    private val nodeSpecs =
        listOf(
            NodeSpec(PC0_NUM, "PC-0", "PC0", LatLon(48.858_400, 2.347_000), 1.minutes, Duration.ZERO, hops = 0),
            NodeSpec(ALPHA1_NUM, "ALPHA-1 [Alpha]", "A1", alpha1Point, alpha1PositionAge, 1.minutes, hops = 0),
            NodeSpec(BRAVO2_NUM, "BRAVO-2 [Bravo]", "B2", LatLon(48.860_100, 2.341_200), 25.minutes, 20.minutes, 2),
            NodeSpec(CHARLIE3_NUM, "CHARLIE-3 [Alpha]", "C3", LatLon(48.853_000, 2.360_000), null, 5.minutes, 1),
        )

    /** The conversations, oldest first within each. */
    private val history: List<Entry> =
        listOf(
            Received(GENERAL_KEY, ALPHA1_NUM, 14.minutes, read = true) { _, _ -> "Point de regroupement atteint" },
            Received(GENERAL_KEY, CHARLIE3_NUM, 12.minutes, read = true) { _, _ -> "[CR] Secteur nord reconnu, RAS" },
            Received(GENERAL_KEY, ALPHA1_NUM, 9.minutes) { _, _ -> "[URG] Blessé léger au point B, demande appui" },
            Sent(GENERAL_KEY, "Bien reçu, appui en route", 8.minutes, MessageStatus.DELIVERED),
            Received(GENERAL_KEY, CHARLIE3_NUM, 6.minutes) { _, _ -> observedFact() },
            Received(GENERAL_KEY, ALPHA1_NUM, alpha1PositionAge) { now, zone -> alpha1Position(now, zone) },
            Sent(GENERAL_KEY, "[CR] Point de situation à 15:00", 1.minutes, MessageStatus.ENROUTE),
            Sent(GENERAL_KEY, "Regroupement au PC à 15:30", 30.seconds, MessageStatus.QUEUED),
            Received(TEAM_ALPHA_KEY, CHARLIE3_NUM, 20.minutes, read = true) { _, _ -> "Prêt au départ" },
            Sent(TEAM_ALPHA_KEY, "Départ dans 5 min", 19.minutes, MessageStatus.DELIVERED),
            Sent(COMMAND_POST_KEY, TeamRoster(TEAMS).toMessage(), rosterAge, MessageStatus.DELIVERED),
            Received(COMMAND_POST_KEY, BRAVO2_NUM, 16.minutes, read = true) { _, _ -> "[CR] Arrivé au point C" },
            Received(BRAVO2_DIRECT_KEY, BRAVO2_NUM, 18.minutes, read = true) { _, _ ->
                "Reçu, en route vers le point C"
            },
            Sent(BRAVO2_DIRECT_KEY, "Confirmez votre position", 17.minutes, MessageStatus.RECEIVED),
            Sent(
                CHARLIE3_DIRECT_KEY,
                "Rejoignez le point B",
                7.minutes,
                MessageStatus.ERROR,
                routingError = Routing.Error.MAX_RETRANSMIT.value,
            ),
        )

    fun id(num: Int): String = NodeAddress.numToDefaultId(num)

    fun create(nowMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): DemoData {
        val nowSeconds = nowMillis / MILLIS_PER_SECOND
        return DemoData(
            myNodeInfo = myNodeInfo(),
            nodes = nodeSpecs.map { it.toNode(nowSeconds) },
            channelSet = channelSet(),
            roster =
            TeamRosterRecord(
                teams = TEAMS,
                source = TeamRosterRecord.Source.BROADCAST,
                timeSeconds = nowSeconds - rosterAge.inWholeSeconds,
            ),
            packets = history.mapIndexed { index, entry -> entry.toPacket(index, nowMillis, timeZone) },
        )
    }

    private fun alpha1Position(nowSeconds: Long, timeZone: TimeZone): String = PositionMessage.build(
        name = "ALPHA-1",
        point = alpha1Point,
        source = PositionSource.RADIO,
        state = PositionState.Fresh(alpha1PositionAge),
        nowEpochSeconds = nowSeconds,
        timeZone = timeZone,
    )

    private fun observedFact(): String = ObservedFactMessage.build(
        place = FormattedCoordinates.of(observedPoint),
        typedAs = CoordinateFormat.DMS,
        description = "véhicule arrêté au carrefour",
    )

    private fun myNodeInfo(): MyNodeInfo = MyNodeInfo(
        myNodeNum = PC0_NUM,
        hasGPS = true,
        model = null,
        firmwareVersion = null,
        couldUpdate = false,
        shouldUpdate = false,
        currentPacketId = 0L,
        messageTimeoutMsec = 0,
        minAppVersion = 0,
        maxChannels = MAX_CHANNELS,
        hasWifi = false,
        channelUtilization = 0f,
        airUtilTx = 0f,
        deviceId = null,
    )

    private fun channelSet(): ChannelSet {
        val settings =
            listOf(GENERAL, TEAM_ALPHA, COMMAND_POST).map { name ->
                ChannelSettings.Builder().also { it.name = name }.build()
            }
        return ChannelSet.Builder().also { it.settings = settings }.build()
    }

    private class NodeSpec(
        val num: Int,
        val longName: String,
        val shortName: String,
        val point: LatLon,
        /** How old the GPS fix is; null when the radio stored the position without any time. */
        val fixAge: Duration?,
        val heardAge: Duration,
        val hops: Int,
    ) {
        fun toNode(nowSeconds: Long): Node {
            val user =
                User.Builder()
                    .also {
                        it.id = id(num)
                        it.long_name = longName
                        it.short_name = shortName
                    }
                    .build()
            val fixSeconds = fixAge?.let { (nowSeconds - it.inWholeSeconds).toInt() }
            val position =
                Position.Builder()
                    .also {
                        it.latitude_i = (point.latitude * COORDINATE_SCALE).toInt()
                        it.longitude_i = (point.longitude * COORDINATE_SCALE).toInt()
                        // Without a fix time neither field is set: the age is unknown and never estimated.
                        if (fixSeconds != null) {
                            it.timestamp = fixSeconds
                            it.time = fixSeconds
                        }
                    }
                    .build()
            val heard = (nowSeconds - heardAge.inWholeSeconds).toInt()
            return Node(num = num, user = user, position = position, lastHeard = heard, hopsAway = hops)
        }
    }

    private sealed interface Entry {
        val contactKey: String
        val age: Duration

        fun toPacket(index: Int, nowMillis: Long, timeZone: TimeZone): DemoPacket
    }

    private class Received(
        override val contactKey: String,
        val from: Int,
        override val age: Duration,
        val read: Boolean = false,
        val text: (nowSeconds: Long, timeZone: TimeZone) -> String,
    ) : Entry {
        override fun toPacket(index: Int, nowMillis: Long, timeZone: TimeZone): DemoPacket {
            val time = nowMillis - age.inWholeMilliseconds
            val isChannel = contactKey.endsWith(NodeAddress.ID_BROADCAST)
            val to = if (isChannel) NodeAddress.ID_BROADCAST else id(PC0_NUM)
            val packet =
                DataPacket(
                    to = to,
                    channel = channelOf(contactKey),
                    text = text(nowMillis / MILLIS_PER_SECOND, timeZone),
                )
                    .apply {
                        this.from = id(this@Received.from)
                        this.time = time
                        id = FIRST_PACKET_ID + index
                        status = MessageStatus.RECEIVED
                    }
            val hops = nodeSpecs.first { it.num == from }.hops
            return DemoPacket(index + 1L, contactKey, packet, time, read, hops)
        }
    }

    private class Sent(
        override val contactKey: String,
        val text: String,
        override val age: Duration,
        val status: MessageStatus,
        val routingError: Int = 0,
    ) : Entry {
        override fun toPacket(index: Int, nowMillis: Long, timeZone: TimeZone): DemoPacket {
            val time = nowMillis - age.inWholeMilliseconds
            val packet =
                DataPacket(to = contactKey.drop(1), channel = channelOf(contactKey), text = text).apply {
                    from = id(PC0_NUM)
                    this.time = time
                    id = FIRST_PACKET_ID + index
                    status = this@Sent.status
                }
            return DemoPacket(index + 1L, contactKey, packet, time, read = true, hopsAway = 0, routingError)
        }
    }

    private fun channelOf(contactKey: String): Int = contactKey.first().digitToInt()
}
