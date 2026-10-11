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
package org.meshtastic.core.model.diagnostic

import org.meshtastic.core.model.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LinkDiagnosticsTest {

    private val nowSeconds = 1_800_000_000L
    private val nowMillis = nowSeconds * 1000

    private fun heardAgo(num: Int, name: String, minutes: Int) =
        HeardNode(num, name, (nowSeconds - minutes * 60L).toInt())

    private val pc0 = 1
    private val euLongFast = LoraSetting("EU_868", "LONG_FAST")

    private fun input(
        nodes: List<HeardNode> = emptyList(),
        connection: ConnectionState = ConnectionState.Connected,
        local: LoraSetting? = euLongFast,
        expected: LoraSetting? = euLongFast,
        battery: Int? = 80,
        lastPacket: Long? = nowMillis - 30_000,
    ) = DiagnosticInput(
        connection = connection,
        lastRadioPacketMillis = lastPacket,
        myNodeNum = pc0,
        nodes = nodes,
        localLora = local,
        expectedLora = expected,
        batteryLevel = battery,
        nowMillis = nowMillis,
    )

    @Test
    fun countsNodesPerWindowWithoutMyOwnRadio() {
        val nodes =
            listOf(
                heardAgo(pc0, "PC-0", 0),
                heardAgo(2, "ALPHA-1", 3),
                heardAgo(3, "BRAVO-2", 12),
                heardAgo(4, "CHARLIE-3", 45),
                heardAgo(5, "DELTA-4", 200),
            )
        val diagnostic = LinkDiagnostics.build(input(nodes))
        assertEquals(HeardCounts(within5Min = 1, within15Min = 2, within60Min = 3), diagnostic.counts)
        assertEquals("ALPHA-1", diagnostic.lastHeard?.name)
        assertEquals(3.minutes, diagnostic.lastHeard?.age)
        assertFalse(diagnostic.noRecentContact)
        assertTrue(diagnostic.suggestions.isEmpty())
    }

    @Test
    fun noNodeHeardSinceEighteenMinutesGivesFactsAndSuggestions() {
        val diagnostic = LinkDiagnostics.build(input(listOf(heardAgo(3, "BRAVO-2", 18), heardAgo(4, "CHARLIE-3", 40))))
        assertEquals(HeardCounts(0, 0, 2), diagnostic.counts)
        assertEquals(LastHeard("BRAVO-2", nowSeconds - 18 * 60, 18.minutes), diagnostic.lastHeard)
        assertTrue(diagnostic.noRecentContact)
        assertEquals(
            listOf(DiagnosticSuggestion.MOVE_HIGHER, DiagnosticSuggestion.CHECK_ANTENNA),
            diagnostic.suggestions,
        )
    }

    @Test
    fun neverHeardAndNoTimeAreNotCounted() {
        val diagnostic = LinkDiagnostics.build(input(listOf(HeardNode(3, "BRAVO-2", 0))))
        assertNull(diagnostic.lastHeard)
        assertEquals(HeardCounts(0, 0, 0), diagnostic.counts)
        assertTrue(diagnostic.noRecentContact)
    }

    @Test
    fun timeAheadOfTheClockToleranceIsLeftOutAndSmallDriftCountsAsNow() {
        val ahead = HeardNode(3, "BRAVO-2", (nowSeconds + 10 * 60).toInt())
        val drift = HeardNode(4, "CHARLIE-3", (nowSeconds + 30).toInt())
        val diagnostic = LinkDiagnostics.build(input(listOf(ahead, drift)))
        assertEquals("CHARLIE-3", diagnostic.lastHeard?.name)
        assertEquals(0.seconds, diagnostic.lastHeard?.age)
        assertEquals(1, diagnostic.counts.within5Min)
    }

    @Test
    fun disconnectedRadioSuggestsCheckingTheLink() {
        val diagnostic = LinkDiagnostics.build(input(connection = ConnectionState.Disconnected, lastPacket = null))
        assertEquals(ConnectionState.Disconnected, diagnostic.connection)
        assertNull(diagnostic.lastRadioPacketAge)
        assertEquals(DiagnosticSuggestion.CHECK_RADIO_LINK, diagnostic.suggestions.first())
    }

    @Test
    fun lastRadioPacketAgeIsMeasuredFromNow() {
        assertEquals(30.seconds, LinkDiagnostics.build(input()).lastRadioPacketAge)
    }

    @Test
    fun loraSettingIsComparedWithTheExpectedOne() {
        assertEquals(LoraCheck.NotReceived, LinkDiagnostics.loraCheck(null, euLongFast))
        assertEquals(LoraCheck.NoExpected(euLongFast), LinkDiagnostics.loraCheck(euLongFast, null))
        assertEquals(LoraCheck.Same(euLongFast), LinkDiagnostics.loraCheck(euLongFast, euLongFast))
        val different = LinkDiagnostics.loraCheck(LoraSetting("EU_868", "MEDIUM_FAST"), euLongFast)
        assertEquals(
            LoraCheck.Different(
                LoraSetting("EU_868", "MEDIUM_FAST"),
                euLongFast,
                regionDiffers = false,
                presetDiffers = true,
            ),
            different,
        )
        val custom = LinkDiagnostics.loraCheck(LoraSetting("EU_868", null), euLongFast)
        assertTrue(custom is LoraCheck.Different && custom.presetDiffers && !custom.regionDiffers)
    }

    @Test
    fun differentOrUnsetLoraSettingIsSuggested() {
        val different =
            LinkDiagnostics.build(
                input(nodes = listOf(heardAgo(2, "ALPHA-1", 1)), local = LoraSetting("US", "LONG_FAST")),
            )
        assertEquals(listOf(DiagnosticSuggestion.CHECK_LORA_SETTINGS), different.suggestions)
        val unset =
            LinkDiagnostics.build(
                input(
                    nodes = listOf(heardAgo(2, "ALPHA-1", 1)),
                    local = LoraSetting("UNSET", "LONG_FAST"),
                    expected = null,
                ),
            )
        assertEquals(listOf(DiagnosticSuggestion.SET_REGION), unset.suggestions)
    }

    @Test
    fun batteryIsReportedAsGiven() {
        assertEquals(BatteryFact.NotReported, LinkDiagnostics.batteryFact(null))
        assertEquals(BatteryFact.ExternalPower, LinkDiagnostics.batteryFact(101))
        assertEquals(BatteryFact.Level(19, low = true), LinkDiagnostics.batteryFact(19))
        assertEquals(BatteryFact.Level(20, low = false), LinkDiagnostics.batteryFact(20))
        val low = LinkDiagnostics.build(input(nodes = listOf(heardAgo(2, "ALPHA-1", 1)), battery = 10))
        assertEquals(listOf(DiagnosticSuggestion.CHARGE_BATTERY), low.suggestions)
    }
}
