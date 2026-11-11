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
package org.meshtastic.feature.map.offline.extract

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class EditionFinderTest {

    private val base = MapSource.Editions("https://build.protomaps.com")
    private val asked = ArrayList<String>()

    /** 11 November 2026, 08:00 UTC: before the day's edition is published (09:03 UTC that day). */
    private val morning = 1_794_384_000_000L

    private fun server(existing: Set<String>, date: String = "Wed, 11 Nov 2026 15:23:29 GMT") = EditionFinder { url ->
        asked += url
        val status = if (existing.any { url.endsWith("/$it.pmtiles") || url == it }) 200 else 404
        RemoteFileInfo(status, "\"e\"", 139_406_074_356L.takeIf { status == 200 }, status == 200, date)
    }

    @Test
    fun takesTodayWhenItExists() = runTest {
        val found = assertIs<EditionResult.Found>(server(setOf("20261111", "20261110")).find(base, morning))
        assertEquals("20261111", found.date)
        assertEquals("https://build.protomaps.com/20261111.pmtiles", found.url)
        assertEquals(1, asked.size)
    }

    @Test
    fun fallsBackDayByDayToTheNewestThatExists() = runTest {
        val found = assertIs<EditionResult.Found>(server(setOf("20261108", "20261105")).find(base, morning))
        assertEquals("20261108", found.date)
        assertEquals(
            listOf("20261111", "20261110", "20261109", "20261108").map { "https://build.protomaps.com/$it.pmtiles" },
            asked,
        )
    }

    @Test
    fun crossesMonthAndYearBoundaries() = runTest {
        assertEquals(
            "20261130",
            assertIs<EditionResult.Found>(server(setOf("20261130")).find(base, 1_796_094_000_000L)).date,
        )
        assertEquals(
            "20261231",
            assertIs<EditionResult.Found>(server(setOf("20261231")).find(base, 1_798_768_800_000L)).date,
        )
    }

    @Test
    fun noEditionAfterEightDays() = runTest {
        assertEquals(EditionResult.NoEdition(8), server(emptySet()).find(base, morning))
        assertEquals(8, asked.size)
        assertEquals("https://build.protomaps.com/20261104.pmtiles", asked.last())
    }

    @Test
    fun aDeadNetworkStopsAtTheFirstRequest() = runTest {
        val finder = EditionFinder {
            asked += it
            throw MapDownloadException.Unreachable()
        }
        assertEquals(EditionResult.Unreachable, finder.find(base, morning))
        assertEquals(1, asked.size)
    }

    @Test
    fun aPhoneClockFarOffIsCorrectedByTheServerDate() = runTest {
        val thirtyDaysAhead = morning + 30 * 86_400_000L
        val found = assertIs<EditionResult.Found>(server(setOf("20261110")).find(base, thirtyDaysAhead))
        assertEquals("20261110", found.date)
        assertEquals(8 + 2, asked.size)

        asked.clear()
        val tenDaysBehind = morning - 10 * 86_400_000L
        assertEquals(
            "20261111",
            assertIs<EditionResult.Found>(server(setOf("20261111")).find(base, tenDaysBehind)).date,
        )
    }

    @Test
    fun aRightClockIsNotRetried() = runTest {
        assertEquals(EditionResult.NoEdition(8), server(emptySet()).find(base, morning + 3_600_000L))
        assertEquals(8, asked.size)
    }

    @Test
    fun aSingleFileIsFoundOrMissing() = runTest {
        val mirror = MapSource.SingleFile("https://maps.team.example/cincu.pmtiles")
        val found = assertIs<EditionResult.Found>(server(setOf(mirror.url)).find(mirror, morning))
        assertNull(found.date)
        assertEquals(EditionResult.FileMissing, server(emptySet()).find(mirror, morning))
    }

    @Test
    fun sourceAddressesAreHttpsOnly() {
        assertEquals(
            MapSource.Editions("https://build.protomaps.com"),
            MapSource.parse(" https://build.protomaps.com/ "),
        )
        assertEquals(
            MapSource.SingleFile("https://m.example/a/cincu.pmtiles"),
            MapSource.parse("https://m.example/a/cincu.pmtiles"),
        )
        assertNull(MapSource.parse("http://build.protomaps.com"))
        assertNull(MapSource.parse("build.protomaps.com"))
        assertNull(MapSource.parse("https://"))
        assertNull(MapSource.parse("https://bad host.example"))
        assertNull(MapSource.parse("https://user@host.example/x"))
        assertEquals("build.protomaps.com", MapSource.DEFAULT.host)
        assertEquals("m.example", MapSource.SingleFile("https://m.example/a/cincu.pmtiles").host)
    }

    @Test
    fun calendarArithmetic() {
        assertEquals("19700101", CivilDate.format(0))
        assertEquals("20261111", CivilDate.format(20_768))
        assertEquals(20_768L, CivilDate.toEpochDay(2026, 11, 11))
        assertEquals("20240229", CivilDate.format(CivilDate.toEpochDay(2024, 2, 29)))
        for (day in -800_000L..800_000L step 997) {
            val (y, m, d) = CivilDate.fromEpochDay(day)
            assertEquals(day, CivilDate.toEpochDay(y, m, d))
        }
        assertEquals(listOf("20261111", "20261110", "20261109"), CivilDate.candidates(morning, 3))
    }

    @Test
    fun httpDates() {
        assertEquals(1_794_410_609_000L, HttpDate.parseEpochMillis("Wed, 11 Nov 2026 15:23:29 GMT"))
        assertNull(HttpDate.parseEpochMillis("2026-11-11T15:23:29Z"))
        assertNull(HttpDate.parseEpochMillis("Wed, 11 Foo 2026 15:23:29 GMT"))
        assertNull(HttpDate.parseEpochMillis("Wed, 11 Nov 2026 15:23 GMT"))
    }
}
