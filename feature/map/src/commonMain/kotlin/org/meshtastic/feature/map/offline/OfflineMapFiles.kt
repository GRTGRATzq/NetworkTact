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
import kotlinx.serialization.json.jsonObject

/*
 * Offline map files (.pmtiles) for the MapLibre map: the rules an import follows and the documents the host writes so a
 * map draws with no network at all. Kept here, in renderer-neutral common code, so they are tested with the rest of
 * this module; the F-Droid host does the file work.
 */

/** File name extension of an offline map. */
const val PMTILES_EXTENSION = "pmtiles"

private const val MEGABYTE = 1024L * 1024L

/** Room left free after an import, whatever the file size: the app, its database and the system need some too. */
private const val MIN_FREE_MARGIN_BYTES = 50 * MEGABYTE

/** Share of the file size kept free on top of it, for a large file. */
private const val FREE_MARGIN_PERCENT = 5

private const val PERCENT = 100

/** Free space an import of a [fileSize]-byte file needs before the copy starts. */
fun requiredFreeBytes(fileSize: Long): Long =
    fileSize + maxOf(MIN_FREE_MARGIN_BYTES, fileSize / PERCENT * FREE_MARGIN_PERCENT)

/**
 * Whether a [fileSize]-byte file fits in [freeBytes]. An unknown size (null: the provider did not say) is let through;
 * the copy then fails cleanly if the storage fills up.
 */
fun hasRoomForImport(fileSize: Long?, freeBytes: Long): Boolean =
    fileSize == null || requiredFreeBytes(fileSize) <= freeBytes

/** The name an imported map is listed under: its file name without the extension, or [fallback] if that is blank. */
fun offlineMapName(fileName: String?, fallback: String): String {
    val base = fileName.orEmpty().trim().removeSuffix(".$PMTILES_EXTENSION").removeSuffix(".PMTILES").trim()
    return base.ifBlank { fallback }
}

/**
 * The attribution shown for an imported tile archive: the one its own metadata declares, or [fallback] — the
 * OpenStreetMap credit, since every archive this app documents how to make is OpenStreetMap data — when it declares
 * none.
 */
fun archiveAttribution(declared: String?, fallback: String): String =
    declared?.trim().takeUnless { it.isNullOrEmpty() } ?: fallback

/**
 * A bundled Protomaps style with every address pointed at files on the device.
 *
 * @param template One of the bundled style documents, whose glyphs, sprite and source URL are left empty.
 * @param archivePath Absolute path of the .pmtiles file.
 * @param assetsDir Absolute path of the directory the bundled fonts and sprites were installed in.
 * @param flavor `light` or `dark`: which sprite sheet goes with the style.
 * @param attribution The credit the map shows for its data.
 */
fun offlineMapStyle(
    template: String,
    archivePath: String,
    assetsDir: String,
    flavor: String,
    attribution: String,
): String {
    val style = Json.parseToJsonElement(template).jsonObject
    val source = style.getValue("sources").jsonObject.getValue(PROTOMAPS_SOURCE).jsonObject
    val localSource =
        JsonObject(
            source +
                mapOf(
                    "url" to JsonPrimitive("pmtiles://file://$archivePath"),
                    "attribution" to JsonPrimitive(attribution),
                ),
        )
    val sources = JsonObject(style.getValue("sources").jsonObject + (PROTOMAPS_SOURCE to localSource))
    return JsonObject(
        style +
            mapOf(
                "glyphs" to JsonPrimitive(offlineGlyphsUrl(assetsDir)),
                "sprite" to JsonPrimitive("file://$assetsDir/sprites/$flavor"),
                "sources" to sources,
            ),
    )
        .toString()
}

/** The glyph URL template of the bundled fonts installed in [assetsDir]. */
fun offlineGlyphsUrl(assetsDir: String): String = "file://$assetsDir/fonts/{fontstack}/{range}.pbf"

/** The source every bundled style draws its tiles from. */
private const val PROTOMAPS_SOURCE = "protomaps"

private const val GLYPH_RANGE_SIZE = 256
private const val GLYPH_RANGE_COUNT = 256

/** Every glyph range MapLibre may ask for (`0-255` … `65280-65535`). */
val glyphRanges: List<String> =
    (0 until GLYPH_RANGE_COUNT).map { index ->
        val start = index * GLYPH_RANGE_SIZE
        "$start-${start + GLYPH_RANGE_SIZE - 1}"
    }

/**
 * A glyph file for [fontStack] and [range] holding no glyph at all.
 *
 * Written for the ranges the app does not ship. Asking for a range that is not there would fail the request, and a text
 * layer whose glyphs fail to load can take the layers after it down with it; an empty range just leaves those
 * characters out. Protobuf encoding of `glyphs { stacks { name, range } }` from MapLibre's glyphs.proto.
 */
fun emptyGlyphRange(fontStack: String, range: String): ByteArray {
    val name = fontStack.encodeToByteArray()
    val rangeBytes = range.encodeToByteArray()
    val stack =
        byteArrayOf(NAME_TAG) + varint(name.size) + name + byteArrayOf(RANGE_TAG) + varint(rangeBytes.size) + rangeBytes
    return byteArrayOf(STACKS_TAG) + varint(stack.size) + stack
}

/** Field 1 (`stacks`), length-delimited. */
private const val STACKS_TAG: Byte = 0x0A

/** Field 1 (`name`), length-delimited. */
private const val NAME_TAG: Byte = 0x0A

/** Field 2 (`range`), length-delimited. */
private const val RANGE_TAG: Byte = 0x12

private const val VARINT_PAYLOAD_MASK = 0x7F
private const val VARINT_CONTINUATION = 0x80
private const val VARINT_SHIFT = 7

private fun varint(value: Int): ByteArray {
    val out = mutableListOf<Byte>()
    var rest = value
    while (rest > VARINT_PAYLOAD_MASK) {
        out += ((rest and VARINT_PAYLOAD_MASK) or VARINT_CONTINUATION).toByte()
        rest = rest ushr VARINT_SHIFT
    }
    out += rest.toByte()
    return out.toByteArray()
}
