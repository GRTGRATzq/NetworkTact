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

import okio.Buffer
import okio.GzipSink
import okio.buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PmTilesMetadataTest {

    private val credit = "© contributeurs OpenStreetMap · Protomaps"

    // Metadata of a Protomaps basemap such as the Cincu extract: the name says it, the attribution says less.
    private val protomaps =
        """{"name":"Protomaps Basemap","type":"baselayer","version":"4","attribution":"© OpenStreetMap"}"""

    private fun gzip(text: String): ByteArray {
        val buffer = Buffer()
        GzipSink(buffer).buffer().use { it.writeUtf8(text) }
        return buffer.readByteArray()
    }

    // --- Which files are Protomaps basemaps

    @Test
    fun aFileNamedProtomapsBasemapIsOne() {
        val metadata = PmTilesMetadata.parse(protomaps)
        assertTrue(metadata!!.isProtomapsBasemap)
        assertEquals("© OpenStreetMap", metadata.attribution)
    }

    @Test
    fun aDescriptionMentioningProtomapsIsEnough() {
        val metadata =
            PmTilesMetadata.parse("""{"name":"Cincu","description":"Extract of the Protomaps daily build"}""")
        assertTrue(metadata!!.isProtomapsBasemap)
    }

    @Test
    fun theNameAndTheDescriptionAreReadWhateverTheirCaseAndSpacing() {
        assertTrue(PmTilesMetadata.parse("""{"name":"  protomaps basemap "}""")!!.isProtomapsBasemap)
        assertTrue(PmTilesMetadata.parse("""{"description":"built with PROTOMAPS"}""")!!.isProtomapsBasemap)
    }

    @Test
    fun anotherFileIsNotOne() {
        val raster = PmTilesMetadata.parse("""{"name":"Cincu orthophoto","description":"Converted from a GeoTIFF"}""")
        assertFalse(raster!!.isProtomapsBasemap)
        assertFalse(PmTilesMetadata(name = null, description = null, attribution = "© IGN").isProtomapsBasemap)
    }

    // --- The credit shown (the three cases)

    @Test
    fun aProtomapsBasemapIsCreditedWithTheAppsTextWhateverItDeclares() {
        assertEquals(credit, importedCredit(PmTilesMetadata.parse(protomaps), credit))
        assertEquals(
            credit,
            importedCredit(PmTilesMetadata(name = "Protomaps Basemap", description = null, attribution = null), credit),
        )
    }

    @Test
    fun anotherFileKeepsTheCreditItDeclares() {
        val declared = PmTilesMetadata.parse("""{"name":"Ortho 2024","attribution":"© IGN 2024"}""")
        assertNull(importedCredit(declared, credit))
        assertEquals("© IGN 2024", declared!!.attribution)
    }

    @Test
    fun anotherFileThatDeclaresNoCreditIsCreditedWithNothing() {
        val silent = PmTilesMetadata.parse("""{"name":"Ortho 2024"}""")
        assertNull(importedCredit(silent, credit))
        assertNull(silent!!.attribution)
        assertNull(PmTilesMetadata.parse("""{"name":"Ortho 2024","attribution":"  "}""")!!.attribution)
    }

    @Test
    fun aFileWhoseMetadataCannotBeReadDeclaresNothing() {
        assertNull(importedCredit(null, credit))
    }

    // --- Reading the metadata section

    @Test
    fun readsAGzipMetadataSectionAsRealFilesStoreIt() {
        val metadata = PmTilesMetadata.decode(gzip(protomaps), internalCompression = 2)
        assertTrue(metadata!!.isProtomapsBasemap)
    }

    @Test
    fun readsAnUncompressedMetadataSection() {
        assertEquals("Ortho", PmTilesMetadata.decode("""{"name":"Ortho"}""".encodeToByteArray(), 1)!!.name)
    }

    @Test
    fun anUnsupportedCompressionOrBrokenBytesGiveNothing() {
        assertNull(PmTilesMetadata.decode(gzip(protomaps), internalCompression = 4))
        assertNull(PmTilesMetadata.decode(gzip(protomaps), internalCompression = 0))
        assertNull(PmTilesMetadata.decode("not gzip".encodeToByteArray(), internalCompression = 2))
        assertNull(PmTilesMetadata.decode("[1,2]".encodeToByteArray(), internalCompression = 1))
        assertNull(PmTilesMetadata.decode("{".encodeToByteArray(), internalCompression = 1))
    }
}
