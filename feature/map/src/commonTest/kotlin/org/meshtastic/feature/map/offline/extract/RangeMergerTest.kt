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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RangeMergerTest {

    private fun copy(source: Long, length: Long, target: Long = source) = TileCopy(source, target, length)

    @Test
    fun touchingCopiesBecomeOneRequest() {
        val ranges = RangeMerger.merge(listOf(copy(100, 50), copy(0, 100), copy(150, 10)), overfetch = 0.0)
        assertEquals(listOf(FetchRange(0, 160, listOf(copy(0, 100), copy(100, 50), copy(150, 10)))), ranges)
    }

    @Test
    fun smallestGapsAreBridgedWithinTheBudget() {
        // 1 000 useful bytes, 5 % = 50 extra bytes: the gaps of 10 and 30 fit, the gap of 200 does not.
        val copies = listOf(copy(0, 250), copy(260, 250), copy(540, 250), copy(990, 250))
        val ranges = RangeMerger.merge(copies, overfetch = 0.05)
        assertEquals(listOf(0L to 790L, 990L to 250L), ranges.map { it.sourceOffset to it.length })
        assertEquals(copies, ranges.flatMap { it.copies })
    }

    @Test
    fun noRequestGrowsBeyondTheLimit() {
        val copies = List(100) { copy(it * 100L, 100) }
        val ranges = RangeMerger.merge(copies, overfetch = 0.5, maxRangeBytes = 1_000)
        assertTrue(ranges.all { it.length <= 1_000 }, ranges.map { it.length }.toString())
        assertEquals(10, ranges.size)
        assertEquals(copies, ranges.flatMap { it.copies })
    }

    @Test
    fun overlappingCopiesShareARequest() {
        val ranges = RangeMerger.merge(listOf(copy(0, 100, 0), copy(50, 100, 100)), overfetch = 0.0, maxRangeBytes = 10)
        assertEquals(1, ranges.size)
        assertEquals(150L, ranges.single().length)
    }
}
