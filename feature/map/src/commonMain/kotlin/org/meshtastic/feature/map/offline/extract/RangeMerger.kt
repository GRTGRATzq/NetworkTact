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

/** One HTTP request: [length] bytes from [sourceOffset] of the source tile data, holding the [copies] to make. */
data class FetchRange(val sourceOffset: Long, val length: Long, val copies: List<TileCopy>)

/**
 * Groups the byte copies of an extract into few requests, as `pmtiles extract` does: copies that touch are always
 * joined, then the smallest gaps between groups are bridged while the extra bytes stay within [overfetch] of the useful
 * ones. The probe measured 63 requests for Cincu (30 km, 12.7 MB) with 5 %. No request grows beyond [maxRangeBytes], so
 * that a retry after a network error repeats little and progress moves regularly.
 */
object RangeMerger {
    const val DEFAULT_OVERFETCH = 0.05
    const val DEFAULT_MAX_RANGE_BYTES = 8L * 1024 * 1024

    fun merge(
        copies: List<TileCopy>,
        overfetch: Double = DEFAULT_OVERFETCH,
        maxRangeBytes: Long = DEFAULT_MAX_RANGE_BYTES,
    ): List<FetchRange> {
        if (copies.isEmpty()) return emptyList()
        val sorted = copies.sortedBy { it.sourceOffset }
        val groups = touchingGroups(sorted, maxRangeBytes)
        val joined = bridgeGaps(groups, (sorted.sumOf { it.length } * overfetch).toLong(), maxRangeBytes)
        return joined.map { group ->
            FetchRange(group.start, group.end - group.start, sorted.subList(group.first, group.last + 1).toList())
        }
    }

    /** Copies [first] to [last] (indices in the sorted list), spanning [start] to [end] of the source. */
    private class Group(val first: Int, var last: Int, val start: Long, var end: Long)

    private fun touchingGroups(sorted: List<TileCopy>, maxRangeBytes: Long): List<Group> {
        val groups = ArrayList<Group>()
        sorted.forEachIndexed { i, copy ->
            val end = copy.sourceOffset + copy.length
            val current = groups.lastOrNull()
            val overlaps = current != null && copy.sourceOffset < current.end
            val touchesWithinLimit =
                current != null && copy.sourceOffset == current.end && end - current.start <= maxRangeBytes
            if (current != null && (overlaps || touchesWithinLimit)) {
                current.last = i
                current.end = maxOf(current.end, end)
            } else {
                groups += Group(i, i, copy.sourceOffset, end)
            }
        }
        return groups
    }

    /** Bridges the gaps between neighbouring groups, smallest first, within [budget] extra bytes. */
    private fun bridgeGaps(groups: List<Group>, budget: Long, maxRangeBytes: Long): List<Group> {
        // Union of neighbours: parent[i] points left towards the group that absorbed group i.
        val parent = IntArray(groups.size) { it }
        val start = LongArray(groups.size) { groups[it].start }
        val end = LongArray(groups.size) { groups[it].end }
        val last = IntArray(groups.size) { groups[it].last }

        fun root(i: Int): Int {
            var r = i
            while (parent[r] != r) r = parent[r]
            var j = i
            while (parent[j] != r) {
                val next = parent[j]
                parent[j] = r
                j = next
            }
            return r
        }

        var left = budget
        val gaps = (0 until groups.size - 1).sortedBy { groups[it + 1].start - groups[it].end }
        for (gapIndex in gaps) {
            val gap = groups[gapIndex + 1].start - groups[gapIndex].end
            if (gap > left) break
            val a = root(gapIndex)
            val b = root(gapIndex + 1)
            if (end[b] - start[a] <= maxRangeBytes) {
                parent[b] = a
                end[a] = end[b]
                last[a] = last[b]
                left -= gap
            }
        }
        return groups.indices.filter { root(it) == it }.map { r -> Group(groups[r].first, last[r], start[r], end[r]) }
    }
}
