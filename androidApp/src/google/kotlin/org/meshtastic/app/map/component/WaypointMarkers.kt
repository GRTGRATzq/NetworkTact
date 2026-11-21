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
package org.meshtastic.app.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberComposeBitmapDescriptor
import com.google.maps.android.compose.rememberUpdatedMarkerState
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.geo.WaypointAffiliation
import org.meshtastic.core.model.geofence.toGeofence
import org.meshtastic.core.model.isLocked
import org.meshtastic.core.model.isModifiableBy
import org.meshtastic.core.model.util.GeoConstants.DEG_D
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.geofence
import org.meshtastic.core.resources.locked
import org.meshtastic.core.ui.component.AffiliationFrames
import org.meshtastic.core.ui.component.affiliationLabel
import org.meshtastic.feature.map.BaseMapViewModel
import org.meshtastic.proto.Waypoint

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun WaypointMarkers(
    displayableWaypoints: List<Waypoint>,
    mapFilterState: BaseMapViewModel.MapFilterState,
    myNodeNum: Int,
    onEditWaypointRequest: (Waypoint) -> Unit,
    onDeleteWaypointRequest: (Waypoint) -> Unit,
    isMyWaypoint: (Int) -> Boolean,
    onShowGeofenceInfo: (Waypoint) -> Unit,
    selectedWaypointId: Int? = null,
) {
    if (mapFilterState.showWaypoints) {
        displayableWaypoints.forEach { waypoint ->
            val markerState =
                rememberUpdatedMarkerState(
                    position = LatLng((waypoint.latitude_i ?: 0) * DEG_D, (waypoint.longitude_i ?: 0) * DEG_D),
                )

            LaunchedEffect(selectedWaypointId) {
                if (selectedWaypointId == waypoint.id) {
                    markerState.showInfoWindow()
                }
            }

            // The Google flavor does not draw the APP-6(C) frames: the affiliation is written as plain text on its
            // colour, so no emoji and no broken character is ever shown.
            val affiliation = WaypointAffiliation.fromIcon(waypoint.icon)
            val affiliationText = stringResource(affiliationLabel(affiliation))
            val icon =
                rememberComposeBitmapDescriptor(affiliation, affiliationText) {
                    Text(
                        text = affiliationText,
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                        Modifier.border(1.dp, AffiliationFrames.Edge)
                            .background(AffiliationFrames.fill(affiliation))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }

            // Non-visual cue: the geofence is otherwise only an orange overlay, so surface it in the marker's
            // accessible snippet for screen-reader and color-challenged users.
            val description = waypoint.description.replace('\n', ' ').replace('\b', ' ')
            val snippet =
                if (waypoint.toGeofence() != null) {
                    val geofenceLabel = stringResource(Res.string.geofence)
                    if (description.isBlank()) geofenceLabel else "$description · $geofenceLabel"
                } else {
                    description
                }

            // Lock cue in the info-window title, so a locked waypoint is identifiable before it is tapped: a word
            // rather than the old 🔒, since no emoji is left on waypoints.
            val cleanName = waypoint.name.replace('\n', ' ').replace('\b', ' ')
            val lockedText = stringResource(Res.string.locked).lowercase()
            val title = if (waypoint.isLocked) "$cleanName ($lockedText)" else cleanName

            Marker(
                state = markerState,
                icon = icon,
                title = title,
                snippet = snippet,
                visible = true,
                onInfoWindowClick = {
                    when {
                        // Foreign geofences: read-only view hosting the receiver-local crossing-alert opt-in.
                        waypoint.toGeofence() != null && !isMyWaypoint(waypoint.id) -> onShowGeofenceInfo(waypoint)

                        waypoint.isModifiableBy(myNodeNum) -> onEditWaypointRequest(waypoint)

                        // Locked to someone else: the editor could never broadcast a change, whether we are connected
                        // or not, but our local copy is still ours to remove — so offer that instead.
                        else -> onDeleteWaypointRequest(waypoint)
                    }
                },
            )
        }
    }
}
