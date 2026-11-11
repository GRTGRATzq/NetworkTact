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

import io.ktor.client.HttpClient

/** The network side of a map download: HEAD requests and range reads on one source. */
interface MapSourceClient {
    suspend fun head(url: String): RemoteFileInfo

    /** Range reads on [url], refused (412) if the file is no longer the one with [etag]. */
    fun ranges(url: String, etag: String?): RangeSource
}

/** [MapSourceClient] over a Ktor [HttpClient]: the host gives it timeouts and an engine, nothing else. */
class KtorMapSourceClient(private val client: HttpClient) : MapSourceClient {
    override suspend fun head(url: String): RemoteFileInfo = HttpRangeSource(client, url).head()

    override fun ranges(url: String, etag: String?): RangeSource = HttpRangeSource(client, url, etag)
}
