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

import kotlin.math.abs

/** Where maps are downloaded from: dated editions under a base address, or one fixed file (a team's mirror). */
sealed interface MapSource {
    /** `<base>/AAAAMMJJ.pmtiles`, one file a day, kept about a week (Protomaps, probe of 11 November 2026). */
    data class Editions(val base: String) : MapSource {
        fun urlFor(date: String) = "$base/$date.pmtiles"
    }

    /** A single `.pmtiles` file. */
    data class SingleFile(val url: String) : MapSource

    /** The host, to name the source in messages without the full address. */
    val host: String
        get() = (if (this is Editions) base else (this as SingleFile).url).removePrefix("https://").substringBefore('/')

    companion object {
        const val DEFAULT_BASE = "https://build.protomaps.com"
        val DEFAULT: MapSource = Editions(DEFAULT_BASE)

        /** Reads what the user typed: https only; an address ending in `.pmtiles` is a single file. Else null. */
        fun parse(text: String): MapSource? {
            val address = text.trim().trimEnd('/')
            val host = address.removePrefix("https://").substringBefore('/')
            val valid =
                address.startsWith("https://") &&
                    host.isNotEmpty() &&
                    address.none { it.isWhitespace() } &&
                    !host.contains('@')
            return when {
                !valid -> null
                address.endsWith(".pmtiles", ignoreCase = true) -> SingleFile(address)
                else -> Editions(address)
            }
        }
    }
}

/** What [EditionFinder.find] found. */
sealed interface EditionResult {
    /** The newest file that answers, [date] as `AAAAMMJJ` (null for a single file). */
    data class Found(val url: String, val date: String?, val info: RemoteFileInfo) : EditionResult

    /** Nothing answered at all: no network, name not resolved, TLS refused. */
    data object Unreachable : EditionResult

    /** The server answers, but none of the [days] candidate dates exists. */
    data class NoEdition(val days: Int) : EditionResult

    /** The single file answered 404 or 410. */
    data object FileMissing : EditionResult
}

/**
 * Finds the newest edition that exists, with no date written anywhere in the app: the candidates are today (UTC) and
 * the [DAYS] - 1 days before, tried newest first with a HEAD each. When none exists and the server's `Date` is more
 * than a day away from the phone's clock, the candidates are computed again once from the server's date (a phone whose
 * clock is wrong). docs/PLAN-CARTE-TELECHARGEMENT.md §8.
 */
class EditionFinder(private val head: suspend (String) -> RemoteFileInfo) {

    suspend fun find(source: MapSource, nowEpochMillis: Long): EditionResult = when (source) {
        is MapSource.SingleFile -> single(source.url)
        is MapSource.Editions -> editions(source, nowEpochMillis)
    }

    private suspend fun single(url: String): EditionResult {
        val info = headOrNull(url) ?: return EditionResult.Unreachable
        return when {
            info.status == OK -> EditionResult.Found(url, null, info)
            else -> EditionResult.FileMissing
        }
    }

    private suspend fun editions(source: MapSource.Editions, nowEpochMillis: Long): EditionResult {
        val tried = HashSet<String>()
        val first = tryDates(source, CivilDate.candidates(nowEpochMillis, DAYS), tried, answered = false)
        if (first !is Attempt.NoneFound) return first.result(tried.size)
        // Nothing within the week of the phone's clock: if the server's clock says otherwise, try its week once.
        val server = first.serverDate?.let { HttpDate.parseEpochMillis(it) }
        val retry =
            if (server != null && abs(server - nowEpochMillis) > DAY_MILLIS) {
                tryDates(source, CivilDate.candidates(server, DAYS), tried, answered = true)
            } else {
                first
            }
        return retry.result(tried.size)
    }

    /** HEADs the [dates] not yet [tried], newest first, until one exists or the network fails. */
    private suspend fun tryDates(
        source: MapSource.Editions,
        dates: List<String>,
        tried: MutableSet<String>,
        answered: Boolean,
    ): Attempt {
        var serverDate: String? = null
        var anyAnswer = answered
        var outcome: Attempt? = null
        val pending = dates.filter { tried.add(it) }.iterator()
        while (outcome == null && pending.hasNext()) {
            val date = pending.next()
            val info = headOrNull(source.urlFor(date))
            if (info == null) {
                outcome = if (anyAnswer) Attempt.NoneFound(serverDate) else Attempt.Dead
            } else {
                anyAnswer = true
                if (serverDate == null) serverDate = info.date
                if (info.status == OK) outcome = Attempt.Hit(EditionResult.Found(source.urlFor(date), date, info))
            }
        }
        return outcome ?: Attempt.NoneFound(serverDate)
    }

    private sealed interface Attempt {
        fun result(days: Int): EditionResult

        data class Hit(val found: EditionResult.Found) : Attempt {
            override fun result(days: Int) = found
        }

        data object Dead : Attempt {
            override fun result(days: Int) = EditionResult.Unreachable
        }

        data class NoneFound(val serverDate: String?) : Attempt {
            override fun result(days: Int) = EditionResult.NoEdition(days)
        }
    }

    /** The answer to a HEAD, or null when nothing answered (network down, name not resolved, TLS refused). */
    @Suppress("SwallowedException") // A dead network is an answer here: "unreachable", reported by the caller.
    private suspend fun headOrNull(url: String): RemoteFileInfo? =
        try {
            head(url)
        } catch (e: MapDownloadException.Unreachable) {
            null
        }

    companion object {
        /** Today and the 7 days before: the editions last about a week. */
        const val DAYS = 8
        private const val OK = 200
        private const val DAY_MILLIS = 86_400_000L
    }
}

/** Dates in UTC without a date library: days since 1970-01-01 to `AAAAMMJJ` and back (H. Hinnant's algorithms). */
@Suppress("MagicNumber") // The constants of the proleptic Gregorian calendar (400-year eras of 146 097 days, …).
internal object CivilDate {
    private const val DAY_MILLIS = 86_400_000L

    /** `AAAAMMJJ` of [epochMillis] and of the [count] - 1 days before it, newest first. */
    fun candidates(epochMillis: Long, count: Int): List<String> {
        val today = epochMillis.floorDiv(DAY_MILLIS)
        return (0 until count).map { format(today - it) }
    }

    fun format(epochDay: Long): String {
        val (year, month, day) = fromEpochDay(epochDay)
        return year.toString().padStart(4, '0') + month.toString().padStart(2, '0') + day.toString().padStart(2, '0')
    }

    fun fromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
        return Triple(year, month, day)
    }

    fun toEpochDay(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = y.floorDiv(400L)
        val yoe = y - era * 400
        val mp = (if (month > 2) month - 3 else month + 9).toLong()
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }
}

/** The HTTP `Date` header (RFC 9110 IMF-fixdate: `Wed, 11 Nov 2026 15:23:29 GMT`). */
@Suppress("MagicNumber") // Positions of the fields of a fixed format.
internal object HttpDate {
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private const val DAY_MILLIS = 86_400_000L
    private const val HOUR_MILLIS = 3_600_000L
    private const val MINUTE_MILLIS = 60_000L
    private const val SECOND_MILLIS = 1_000L
    private const val FIELDS = 6
    private const val CLOCK_FIELDS = 3

    /** Milliseconds since 1970 of [text], or null when it is not an IMF-fixdate. */
    fun parseEpochMillis(text: String): Long? {
        val parts = text.trim().split(' ')
        if (parts.size != FIELDS || parts[5] != "GMT") return null
        val day = parts[1].toIntOrNull()
        val month = MONTHS.indexOf(parts[2]) + 1
        val year = parts[3].toIntOrNull()
        val clock = parts[4].split(':').mapNotNull { it.toIntOrNull() }
        return if (day == null || year == null || month == 0 || clock.size != CLOCK_FIELDS) {
            null
        } else {
            CivilDate.toEpochDay(year, month, day) * DAY_MILLIS +
                clock[0] * HOUR_MILLIS +
                clock[1] * MINUTE_MILLIS +
                clock[2] * SECOND_MILLIS
        }
    }
}
