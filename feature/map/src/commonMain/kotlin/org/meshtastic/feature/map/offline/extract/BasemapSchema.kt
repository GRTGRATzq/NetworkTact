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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Whether the tiles of a source suit the Protomaps style shipped in the app. */
sealed interface SchemaCheck {
    data object Compatible : SchemaCheck

    /** The metadata's `version` is not a 4.x schema. */
    data class UnknownVersion(val version: String?) : SchemaCheck

    /** Layers the style draws that the tiles do not have. */
    data class MissingLayers(val layers: List<String>) : SchemaCheck

    /** No readable JSON metadata. */
    data object Unreadable : SchemaCheck
}

/**
 * The tile schema the bundled style (Protomaps Basemaps 5.7.2, androidApp/src/fdroid/assets/offline-map/styles) was
 * made for. The probe of 11 November 2026 measured `version` 4.15.2 and exactly these 9 layers in the daily build: a
 * download is allowed for a 4.x schema with all of them, refused otherwise with a message, rather than producing a map
 * that draws nothing.
 */
object BasemapSchema {
    /** The `source-layer`s of the light and dark styles. */
    val STYLE_LAYERS =
        listOf("boundaries", "buildings", "earth", "landcover", "landuse", "places", "pois", "roads", "water")
    private const val SUPPORTED_MAJOR = "4"
    private val json = Json { ignoreUnknownKeys = true }

    fun check(metadataJson: String): SchemaCheck {
        val root = runCatching { json.parseToJsonElement(metadataJson) as? JsonObject }.getOrNull()
        val version = (root?.get("version") as? JsonPrimitive)?.contentOrNull
        val layers =
            (root?.get("vector_layers") as? JsonArray)
                ?.mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull }
                .orEmpty()
        val missing = STYLE_LAYERS.filter { it !in layers }
        return when {
            root == null -> SchemaCheck.Unreadable
            version?.substringBefore('.') != SUPPORTED_MAJOR -> SchemaCheck.UnknownVersion(version)
            missing.isNotEmpty() -> SchemaCheck.MissingLayers(missing)
            else -> SchemaCheck.Compatible
        }
    }
}
