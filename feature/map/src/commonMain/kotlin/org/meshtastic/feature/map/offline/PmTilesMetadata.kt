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
package org.meshtastic.feature.map.offline

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okio.Buffer
import okio.GzipSource
import okio.buffer

/**
 * The few keys of a PMTiles archive's JSON metadata that decide how it is credited. The metadata is read once, when the
 * maps on the device are listed.
 *
 * @property attribution What the file declares, usually HTML (`<a href=…>Protomaps</a> © <a href=…>OpenStreetMap</a>`).
 */
data class PmTilesMetadata(val name: String?, val description: String?, val attribution: String?) {

    /**
     * Whether the file is a Protomaps basemap: its `name` is "Protomaps Basemap", or its `description` mentions
     * Protomaps. Any other file (a raster map converted from a GeoTIFF, say) comes with its own source.
     */
    val isProtomapsBasemap: Boolean
        get() =
            name?.trim().equals(PROTOMAPS_BASEMAP_NAME, ignoreCase = true) ||
                description?.contains(PROTOMAPS, ignoreCase = true) == true

    companion object {
        /** A metadata section larger than this is not read: real ones are a few kilobytes. */
        const val MAX_BYTES = 1_048_576L

        private const val PROTOMAPS_BASEMAP_NAME = "Protomaps Basemap"
        private const val PROTOMAPS = "Protomaps"
        private const val COMPRESSION_NONE = 1
        private const val COMPRESSION_GZIP = 2

        private val json = Json { ignoreUnknownKeys = true }

        /** The keys of [text], or null when it is not a JSON object. */
        fun parse(text: String): PmTilesMetadata? {
            val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
            fun string(key: String) = (root[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            return PmTilesMetadata(string("name"), string("description"), string("attribution"))
        }

        /**
         * The metadata section [bytes], compressed as [internalCompression] says. Null for a compression that is not
         * read here (brotli, zstd, unknown) or for bytes that do not decode: the file is then treated as declaring
         * nothing.
         */
        fun decode(bytes: ByteArray, internalCompression: Int): PmTilesMetadata? {
            val text =
                when (internalCompression) {
                    COMPRESSION_NONE -> bytes.decodeToString()

                    COMPRESSION_GZIP ->
                        runCatching { GzipSource(Buffer().write(bytes)).buffer().readUtf8() }.getOrNull()

                    else -> null
                }
            return text?.let(::parse)
        }
    }
}

/**
 * The credit the ⓘ button must show for an archive, in place of what its file declares: [protomapsCredit] for a
 * Protomaps basemap, whatever its metadata says (it often says only "© OpenStreetMap"), and null for any other file,
 * which keeps the credit it declares itself, or none if it declares none.
 */
fun importedCredit(metadata: PmTilesMetadata?, protomapsCredit: String): String? =
    protomapsCredit.takeIf { metadata?.isProtomapsBasemap == true }
