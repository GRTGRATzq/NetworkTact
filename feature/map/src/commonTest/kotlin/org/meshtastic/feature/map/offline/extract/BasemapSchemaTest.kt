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

class BasemapSchemaTest {

    private fun metadata(version: String?, layers: List<String>) = buildString {
        append("{")
        if (version != null) append("\"version\":\"$version\",")
        append("\"name\":\"Protomaps Basemap\",\"vector_layers\":[")
        append(layers.joinToString(",") { "{\"id\":\"$it\",\"fields\":{}}" })
        append("]}")
    }

    @Test
    fun theDailyBuildOfNovember2026IsCompatible() {
        assertEquals(SchemaCheck.Compatible, BasemapSchema.check(metadata("4.15.2", BasemapSchema.STYLE_LAYERS)))
        assertEquals(
            SchemaCheck.Compatible,
            BasemapSchema.check(metadata("4.0.0", BasemapSchema.STYLE_LAYERS + "extra")),
        )
    }

    @Test
    fun anotherMajorVersionIsRefused() {
        assertEquals(
            SchemaCheck.UnknownVersion("5.0.0"),
            BasemapSchema.check(metadata("5.0.0", BasemapSchema.STYLE_LAYERS)),
        )
        assertEquals(SchemaCheck.UnknownVersion(null), BasemapSchema.check(metadata(null, BasemapSchema.STYLE_LAYERS)))
    }

    @Test
    fun missingLayersAreNamed() {
        assertEquals(
            SchemaCheck.MissingLayers(listOf("buildings", "pois")),
            BasemapSchema.check(metadata("4.15.2", BasemapSchema.STYLE_LAYERS - "buildings" - "pois")),
        )
    }

    @Test
    fun garbageIsUnreadable() {
        assertEquals(SchemaCheck.Unreadable, BasemapSchema.check("not json"))
        assertEquals(SchemaCheck.Unreadable, BasemapSchema.check("[1,2]"))
    }
}
