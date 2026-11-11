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

/**
 * Why a map download stopped. Each case has its own message on the phone (docs/PLAN-CARTE-TELECHARGEMENT.md §11 and
 * §12); none carries the source address or the region, which never go to the logs.
 */
sealed class MapDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** No answer at all: no network, name not resolved, TLS refused, timeout, connection cut. Worth a retry later. */
    class Unreachable(cause: Throwable? = null) : MapDownloadException("source unreachable", cause)

    /** 404 or 410: the edition was removed (they last about a week) or the address is wrong. */
    class NotFound(val status: Int) : MapDownloadException("not found ($status)")

    /** 412, or another ETag than the first answer's: the source file changed during the download. */
    class EditionChanged : MapDownloadException("the source changed during the download")

    /** 200 instead of 206, 416, or a Content-Range that does not match: the server cannot serve parts of the file. */
    class RangesUnsupported(detail: String) : MapDownloadException("partial downloads unsupported: $detail")

    /** 429 or 5xx still after every retry. */
    class ServerBusy(val status: Int) : MapDownloadException("server busy ($status)")

    /** Any other HTTP status. */
    class UnexpectedStatus(val status: Int) : MapDownloadException("unexpected HTTP status $status")

    /** The source is a PMTiles archive this app cannot extract from (version, compression, not clustered, raster). */
    class UnsupportedSource(detail: String, cause: Throwable? = null) :
        MapDownloadException("unsupported source: $detail", cause)

    /** The finished file fails its check (length, header, directories or a sample tile): nothing is kept. */
    class Incomplete(detail: String, cause: Throwable? = null) : MapDownloadException("incomplete map: $detail", cause)
}
