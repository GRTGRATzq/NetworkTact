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
package org.meshtastic.core.model.firerequest

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GdhTest {

    private val paris = TimeZone.of("Europe/Paris")
    private val bucharest = TimeZone.of("Europe/Bucharest")

    @Test
    fun specification_example_in_the_three_zones() {
        // 11 November 2026, 21:50 UTC: 22:50 in France (winter time), 23:50 in Romania.
        val now = 1_794_433_800L
        assertEquals("112250 loc NOV 26", Gdh.of(now, GdhZone.LOCAL, paris).format())
        assertEquals("112150Z NOV 26", Gdh.of(now, GdhZone.ZULU, paris).format())
        assertEquals("112250 fra NOV 26", Gdh.of(now, GdhZone.FRANCE, bucharest).format())
        assertEquals("112350 loc NOV 26", Gdh.of(now, GdhZone.LOCAL, bucharest).format())
    }

    @Test
    fun day_changes_between_zulu_and_french_time() {
        // 11 November 2026, 23:30 UTC: already 12 November in France.
        val now = 1_794_439_800L
        assertEquals("112330Z NOV 26", Gdh.of(now, GdhZone.ZULU, paris).format())
        assertEquals("120030 fra NOV 26", Gdh.of(now, GdhZone.FRANCE, paris).format())
    }

    @Test
    fun month_and_year_change_with_the_zone() {
        // 31 December 2026, 23:30 UTC: 1 January 2027 in France.
        val now = 1_798_759_800L
        assertEquals("312330Z DEC 26", Gdh.of(now, GdhZone.ZULU, paris).format())
        assertEquals("010030 fra JAN 27", Gdh.of(now, GdhZone.FRANCE, paris).format())
    }

    @Test
    fun french_summer_time_is_two_hours_ahead_of_zulu() {
        // 14 July 2026, 10:00 UTC: 12:00 in France (summer time).
        val now = 1_784_023_200L
        assertEquals("141000Z JUL 26", Gdh.of(now, GdhZone.ZULU, paris).format())
        assertEquals("141200 fra JUL 26", Gdh.of(now, GdhZone.FRANCE, TimeZone.UTC).format())
    }

    @Test
    fun months_are_the_nato_abbreviations() {
        assertEquals(
            listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"),
            Gdh.MONTHS,
        )
    }

    @Test
    fun written_forms_read_back() {
        for (text in listOf("112250 loc NOV 26", "112150Z NOV 26", "112250 fra NOV 26", "010000Z JAN 27")) {
            assertEquals(text, Gdh.parse(text)?.format(), text)
        }
    }

    @Test
    fun reading_ignores_case_and_extra_spaces() {
        assertEquals(Gdh(11, 22, 50, GdhZone.LOCAL, 11, 2026), Gdh.parse("  112250  LOC nov 26 "))
        assertEquals(Gdh(11, 21, 50, GdhZone.ZULU, 11, 2026), Gdh.parse("112150z nov 26"))
    }

    @Test
    fun impossible_dates_and_times_do_not_read() {
        assertNull(Gdh.parse("322250 loc NOV 26"), "day 32")
        assertNull(Gdh.parse("312250 loc NOV 26"), "31 November")
        assertNull(Gdh.parse("002250 loc NOV 26"), "day 0")
        assertNull(Gdh.parse("292250 loc FEB 26"), "29 February 2026")
        assertNull(Gdh.parse("112450 loc NOV 26"), "hour 24")
        assertNull(Gdh.parse("112260 loc NOV 26"), "minute 60")
        assertNull(Gdh.parse("112250 loc NOE 26"), "unknown month")
        assertNull(Gdh.parse("112250 loc NOV 2026"), "four-digit year")
        assertNull(Gdh.parse("112250 Z NOV 26"), "Zulu is joined to the time")
        assertNull(Gdh.parse("112250loc NOV 26"), "loc is spaced")
        assertNull(Gdh.parse("112250 utc NOV 26"), "unknown zone")
        assertNull(Gdh.parse("22h50"), "time alone")
        assertNull(Gdh.parse(""), "empty")
    }

    @Test
    fun leap_day_reads_in_a_leap_year() {
        assertEquals(Gdh(29, 12, 0, GdhZone.ZULU, 2, 2028), Gdh.parse("291200Z FEB 28"))
        assertEquals("291200Z FEB 28", Gdh.of(1_835_438_400L, GdhZone.ZULU, paris).format())
    }
}
