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
package org.meshtastic.feature.map.maplibre.geojson

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.geo.WaypointAffiliation
import org.meshtastic.core.model.geofence.toGeofence

/** Scale factor turning firmware's fixed-point degrees into floating-point degrees. */
private const val DEG_SCALE = 1e-7

/** Property keys carried on waypoint features. */
object WaypointFeatureKeys {
    const val WAYPOINT_ID = "waypointId"
    const val NAME = "name"
    const val AFFILIATION = "affiliation"
    const val IS_LOCKED = "isLocked"
    const val IS_SELECTED = "isSelected"

    /** Which of the rasterized frames the feature is drawn with: [waypointLook] of its affiliation and selection. */
    const val LOOK = "look"
}

/** The key of one waypoint image: its affiliation, ringed or not. Eight looks in all, whatever the number of points. */
fun waypointLook(affiliation: WaypointAffiliation, selected: Boolean): String =
    if (selected) "${affiliation.name}-selected" else affiliation.name

/**
 * Waypoint markers.
 *
 * The `icon` field is emitted as the name of its [WaypointAffiliation] (Unknown for anything not recognised), and
 * whether the waypoint's card is open as [WaypointFeatureKeys.IS_SELECTED]: the layers group on both, one symbol layer
 * and one rasterized frame per look (see [WaypointLayers][org.meshtastic.feature.map.maplibre.layers.WaypointLayers]).
 */
fun waypointsToFeatureCollection(
    waypoints: Collection<DataPacket>,
    selectedWaypointId: Int? = null,
): FeatureCollection<Point, JsonObject?> = FeatureCollection(
    waypoints.mapNotNull { packet ->
        val waypoint = packet.waypoint ?: return@mapNotNull null
        // Both axes required, not just "not null island". Substituting 0 for one absent ordinate put a marker on
        // the equator or the prime meridian at the other's position — a confident pin in the wrong place, which is
        // worse than no pin. A waypoint genuinely at 0,0 is also dropped, as it was before.
        val latitudeI = waypoint.latitude_i ?: return@mapNotNull null
        val longitudeI = waypoint.longitude_i ?: return@mapNotNull null
        val latitude = latitudeI * DEG_SCALE
        val longitude = longitudeI * DEG_SCALE
        if (latitude == 0.0 && longitude == 0.0) return@mapNotNull null
        Feature(
            geometry = Point(Position(longitude = longitude, latitude = latitude)),
            properties =
            buildJsonObject {
                put(WaypointFeatureKeys.WAYPOINT_ID, waypoint.id)
                put(WaypointFeatureKeys.NAME, waypoint.name)
                val affiliation = WaypointAffiliation.fromIcon(waypoint.icon)
                val selected = selectedWaypointId != null && waypoint.id == selectedWaypointId
                put(WaypointFeatureKeys.AFFILIATION, affiliation.name)
                put(WaypointFeatureKeys.IS_LOCKED, (waypoint.locked_to ?: 0) != 0)
                put(WaypointFeatureKeys.IS_SELECTED, selected)
                put(WaypointFeatureKeys.LOOK, waypointLook(affiliation, selected))
            },
        )
    },
)

/**
 * Geofence zones attached to waypoints, as real ground polygons.
 *
 * Circles are tessellated with the same helper the precision circles use, so a geofence drawn here and the
 * [org.meshtastic.core.model.geofence.Geofence.contains] test the alert engine runs agree about where the boundary is.
 */
fun geofencesToFeatureCollection(waypoints: Collection<DataPacket>): FeatureCollection<Geometry, JsonObject?> =
    FeatureCollection(
        waypoints.flatMap { packet ->
            val waypoint = packet.waypoint ?: return@flatMap emptyList()
            val geofence = waypoint.toGeofence() ?: return@flatMap emptyList()
            buildList {
                geofence.circle?.let { circle ->
                    add(
                        Feature<Geometry, JsonObject?>(
                            geometry =
                            circlePolygon(circle.centerLat, circle.centerLon, circle.radiusMeters.toDouble()),
                            properties = buildJsonObject { put(WaypointFeatureKeys.WAYPOINT_ID, waypoint.id) },
                        ),
                    )
                }
                geofence.box?.let { box ->
                    val ring =
                        listOf(
                            Position(longitude = box.west, latitude = box.south),
                            Position(longitude = box.west, latitude = box.north),
                            Position(longitude = box.east, latitude = box.north),
                            Position(longitude = box.east, latitude = box.south),
                            Position(longitude = box.west, latitude = box.south),
                        )
                    add(
                        Feature<Geometry, JsonObject?>(
                            geometry = Polygon(listOf(ring)),
                            properties = buildJsonObject { put(WaypointFeatureKeys.WAYPOINT_ID, waypoint.id) },
                        ),
                    )
                }
            }
        },
    )
