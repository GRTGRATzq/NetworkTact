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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineMapFilesTest {

    private val megabyte = 1024L * 1024L

    @Test
    fun smallFileKeepsFiftyMegabytesFree() {
        assertEquals(10 * megabyte + 50 * megabyte, requiredFreeBytes(10 * megabyte))
    }

    @Test
    fun largeFileKeepsFivePercentFree() {
        val twoGigabytes = 2_048 * megabyte
        assertEquals(twoGigabytes + twoGigabytes / 100 * 5, requiredFreeBytes(twoGigabytes))
    }

    @Test
    fun importRefusedWhenStorageTooSmall() {
        assertFalse(hasRoomForImport(fileSize = 500 * megabyte, freeBytes = 520 * megabyte))
        assertTrue(hasRoomForImport(fileSize = 500 * megabyte, freeBytes = 600 * megabyte))
    }

    @Test
    fun importOfUnknownSizeIsLetThrough() {
        assertTrue(hasRoomForImport(fileSize = null, freeBytes = 0))
    }

    @Test
    fun mapNameIsFileNameWithoutExtension() {
        assertEquals("Camp de Canjuers", offlineMapName("Camp de Canjuers.pmtiles", fallback = "Carte"))
        assertEquals("dept83", offlineMapName("dept83.PMTILES", fallback = "Carte"))
        assertEquals("Carte", offlineMapName(".pmtiles", fallback = "Carte"))
        assertEquals("Carte", offlineMapName(null, fallback = "Carte"))
    }

    @Test
    fun archiveAttributionFallsBackToOpenStreetMapCredit() {
        val credit = "© contributeurs OpenStreetMap"
        assertEquals(credit, archiveAttribution(null, credit))
        assertEquals(credit, archiveAttribution("  ", credit))
        assertEquals(
            "© OpenStreetMap contributors, IGN",
            archiveAttribution(" © OpenStreetMap contributors, IGN ", credit),
        )
    }

    private val template =
        """{"version":8,"glyphs":"","sprite":"","sources":{"protomaps":{"type":"vector","url":"",""" +
            """"attribution":"x"}},"layers":[{"id":"earth","type":"fill","source":"protomaps"}]}"""

    @Test
    fun styleReadsEverythingFromDevice() {
        val style =
            Json.parseToJsonElement(
                offlineMapStyle(
                    template = template,
                    archivePath = "/data/user/0/app/files/pmtiles/a1.pmtiles",
                    assetsDir = "/data/user/0/app/files/offline-map/v1",
                    flavor = "dark",
                    attribution = "© contributeurs OpenStreetMap",
                ),
            )
                .jsonObject
        val source = style.getValue("sources").jsonObject.getValue("protomaps").jsonObject
        assertEquals(
            "pmtiles://file:///data/user/0/app/files/pmtiles/a1.pmtiles",
            source.getValue("url").jsonPrimitive.content,
        )
        assertEquals("vector", source.getValue("type").jsonPrimitive.content)
        assertEquals("© contributeurs OpenStreetMap", source.getValue("attribution").jsonPrimitive.content)
        assertEquals(
            "file:///data/user/0/app/files/offline-map/v1/fonts/{fontstack}/{range}.pbf",
            style.getValue("glyphs").jsonPrimitive.content,
        )
        assertEquals(
            "file:///data/user/0/app/files/offline-map/v1/sprites/dark",
            style.getValue("sprite").jsonPrimitive.content,
        )
        assertEquals(1, style.getValue("layers").jsonArray.size)
        assertFalse(style.toString().contains("http"))
    }

    @Test
    fun glyphRangesCoverEveryCodePoint() {
        assertEquals(256, glyphRanges.size)
        assertEquals("0-255", glyphRanges.first())
        assertEquals("65280-65535", glyphRanges.last())
    }

    @Test
    fun emptyGlyphRangeIsValidProtobuf() {
        val bytes = emptyGlyphRange("Noto Sans Regular", "0-255")
        val name = "Noto Sans Regular".encodeToByteArray()
        val range = "0-255".encodeToByteArray()
        val stack = byteArrayOf(0x0A, name.size.toByte()) + name + byteArrayOf(0x12, range.size.toByte()) + range
        assertContentEquals(byteArrayOf(0x0A, stack.size.toByte()) + stack, bytes)
    }

    @Test
    fun emptyGlyphRangeEncodesLongLengthsOnSeveralBytes() {
        val bytes = emptyGlyphRange("N".repeat(200), "0-255")
        // Stack: tag 1 + length 2 (200 needs two bytes) + 200 + tag 1 + length 1 + 5 = 210, sent as 0xD2 0x01.
        assertContentEquals(byteArrayOf(0x0A, 0xD2.toByte(), 0x01), bytes.copyOf(3))
    }
}
