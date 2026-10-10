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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.geo.MapPointCoordinate
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.ui.util.MapNavigationRequests
import org.meshtastic.core.ui.util.createClipEntry

/** What can be done with a point of the map; [onCreateWaypoint] is null where no waypoint can be placed. */
internal class MapPointActions(
    val onCopy: () -> Unit,
    val onInsert: () -> Unit,
    val onOpenConverter: () -> Unit,
    val onCreateWaypoint: (() -> Unit)?,
    val onDismiss: () -> Unit,
)

/** The point long-pressed on the map, while its card is open. */
@Stable
internal class MapPointState {
    var point by mutableStateOf<MapPointCoordinate?>(null)
        private set

    /** Where the press was, as the map reported it: where a waypoint created from the card goes. */
    var pressed by mutableStateOf<Position?>(null)
        private set

    var copied by mutableStateOf(false)

    fun show(position: Position) {
        point = MapPointCoordinate.of(position.latitude, position.longitude) ?: return
        pressed = position
        copied = false
    }

    /**
     * What a long press on the map does: show the pressed point's coordinate. Not while a geofence box is drawn, since
     * every press on the map belongs to that flow then.
     */
    fun onLongPress(waypoints: WaypointEditing): (Position) -> Unit = { position ->
        if (waypoints.boxDraft == null) show(position)
    }

    fun dismiss() {
        point = null
        pressed = null
        copied = false
    }
}

@Composable internal fun rememberMapPointState(): MapPointState = remember { MapPointState() }

/**
 * The card of the long-pressed point: copy its coordinate, insert it into a message (the user then picks the
 * conversation; nothing is sent), open it in the converter, or place a waypoint there where [waypoints] allows it.
 */
@Composable
internal fun MapPointSlot(state: MapPointState, waypoints: WaypointEditing) {
    val point = state.point ?: return
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    MapPointCard(
        point = point,
        copied = state.copied,
        actions =
        MapPointActions(
            onCopy = {
                scope.launch {
                    clipboard.setClipEntry(createClipEntry(point.messageText, MapPointCoordinate.PREFIX))
                    state.copied = true
                }
            },
            onInsert = { MapNavigationRequests.open(ContactsRoute.Share(point.messageText)) },
            onOpenConverter = {
                val input = point.formatted.mgrs ?: point.formatted.dms
                MapNavigationRequests.open(ContactsRoute.CoordinateConverter(input))
            },
            onCreateWaypoint =
            state.pressed
                ?.takeIf { waypoints.canCreate }
                ?.let { pressed ->
                    {
                        waypoints.onLongPress(pressed)
                        state.dismiss()
                    }
                },
            onDismiss = state::dismiss,
        ),
    )
}
