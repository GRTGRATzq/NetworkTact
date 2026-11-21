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
package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.expressions.dsl.asBoolean
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.condition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.dsl.textOffset
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.geo.WaypointAffiliation
import org.meshtastic.core.ui.component.AffiliationFrameGeometry
import org.meshtastic.core.ui.component.AffiliationFrames
import org.meshtastic.core.ui.component.drawAffiliationFrame
import org.meshtastic.feature.map.maplibre.geojson.WaypointFeatureKeys
import org.meshtastic.feature.map.maplibre.geojson.geofencesToFeatureCollection
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.maplibre.geojson.waypointLook
import org.meshtastic.feature.map.maplibre.geojson.waypointsToFeatureCollection
import org.meshtastic.feature.map.maplibre.style.MapColors

private const val GEOFENCE_FILL_OPACITY = 0.12f

/**
 * Waypoint markers and their geofence zones.
 *
 * Zones are drawn first so a marker is never hidden behind its own boundary fill. Each waypoint is its APP-6(C)
 * affiliation frame, empty, with its name beside it; the one whose card is open ([selectedWaypointId]) is ringed and
 * drawn above the others.
 */
@Composable
internal fun WaypointLayers(
    waypoints: Collection<DataPacket>,
    onWaypointClick: (Int) -> Unit,
    selectedWaypointId: Int? = null,
) {
    val geofenceSource = rememberFeatureSource(waypoints) { geofencesToFeatureCollection(waypoints) }

    FillLayer(
        id = "geofence-fill",
        source = geofenceSource,
        color = const(MapColors.Highlight),
        opacity = const(GEOFENCE_FILL_OPACITY),
    )

    LineLayer(id = "geofence-outline", source = geofenceSource, color = const(MapColors.Highlight), width = const(2.dp))

    val waypointSource =
        rememberFeatureSource(waypoints, selectedWaypointId) {
            waypointsToFeatureCollection(waypoints, selectedWaypointId)
        }

    // One image per look (four frames, ringed or not), whatever the number of waypoints: the map binds them once, and
    // [WaypointFeatureKeys.LOOK] picks one per feature.
    val frames = rememberWaypointFrames()

    SymbolLayer(
        id = "waypoint-icon",
        source = waypointSource,
        iconImage =
        switch(
            input = feature[WaypointFeatureKeys.LOOK].asString(),
            *frames.map { (look, frame) -> case(look, image(frame.painter, frame.size)) }.toTypedArray(),
            fallback =
            frames.getValue(waypointLook(WaypointAffiliation.UNKNOWN, false)).let {
                image(it.painter, it.size)
            },
        ),
        iconAllowOverlap = const(true),
        // With overlap allowed, higher keys draw on top: the waypoint whose card is open stays visible.
        sortKey =
        switch(condition(feature[WaypointFeatureKeys.IS_SELECTED].asBoolean(), const(1f)), fallback = const(0f)),
        onClick = { features ->
            features.firstOrNull()?.properties?.get(WaypointFeatureKeys.WAYPOINT_ID)?.let { id ->
                onWaypointClick(id.jsonPrimitive.int)
                ClickResult.Consume
            } ?: ClickResult.Pass
        },
    )

    // The name beside the frame, never inside it: left-aligned just past the frame's right edge.
    SymbolLayer(
        id = "waypoint-label",
        source = waypointSource,
        textField = feature[WaypointFeatureKeys.NAME].asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textColor = const(Color.White),
        textHaloColor = const(Color.Black),
        textHaloWidth = const(1.dp),
        textAnchor = const(SymbolAnchor.Left),
        textOffset =
        switch(
            input = feature[WaypointFeatureKeys.AFFILIATION].asString(),
            *WaypointAffiliation.entries
                .map { case(it.name, textOffset(labelOffsetEm(it).em, 0f.em)) }
                .toTypedArray(),
            fallback = textOffset(labelOffsetEm(WaypointAffiliation.UNKNOWN).em, 0f.em),
        ),
    )
}

/** Half the frame's image, plus a small gap, in ems of the label's 16 sp text (MapLibre's text-offset unit). */
private fun labelOffsetEm(affiliation: WaypointAffiliation): Float =
    (AffiliationFrames.outerSize(affiliation).width.value / 2 + LABEL_GAP_DP) / LABEL_EM_DP

private const val LABEL_GAP_DP = 2f
private const val LABEL_EM_DP = 16f

/** A rasterized frame and the size MapLibre should draw it at. */
private class WaypointFrame(val painter: Painter, val size: DpSize)

/**
 * Rasterizes the eight looks once. One `remember` over the whole set: [Painter] identity is part of the key
 * maplibre-compose reference-counts style images by, so a painter rebuilt on recomposition would re-upload them.
 */
@Composable
private fun rememberWaypointFrames(): Map<String, WaypointFrame> {
    val density = LocalDensity.current
    return remember(density) {
        WaypointAffiliation.entries
            .flatMap { affiliation -> listOf(false, true).map { selected -> affiliation to selected } }
            .associate { (affiliation, selected) ->
                waypointLook(affiliation, selected) to
                    WaypointFrame(
                        painter = AffiliationFramePainter(density, affiliation, selected),
                        size = AffiliationFrames.outerSize(affiliation),
                    )
            }
    }
}

/** Draws exactly what [org.meshtastic.core.ui.component.AffiliationFrame] draws, for the map's style images. */
private class AffiliationFramePainter(
    density: Density,
    affiliation: WaypointAffiliation,
    private val selected: Boolean,
) : Painter() {
    private val geometry = AffiliationFrameGeometry(density, affiliation)

    override val intrinsicSize: Size = Size(geometry.outerWidthPx.toFloat(), geometry.outerHeightPx.toFloat())

    override fun DrawScope.onDraw() {
        drawAffiliationFrame(geometry = geometry, selected = selected)
    }
}
