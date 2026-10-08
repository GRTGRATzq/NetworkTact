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

import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.maplibre.style.Basemaps

/**
 * The basemap to draw: the stored custom source while it exists; when it has gone (a map file deleted), another map
 * stored on the device if there is one, so a phone that relies on a local map keeps one; otherwise the stored built-in
 * style. A built-in style the user chose explicitly is stored as no custom id at all, so it is never overridden.
 */
internal fun resolveBasemap(storedCustomId: String?, styleIndex: Int, customs: List<Basemap>): Basemap {
    val builtIn = Basemaps.all.getOrElse(styleIndex) { Basemaps.default }
    if (storedCustomId == null) return builtIn
    return customs.firstOrNull { it.id == storedCustomId } ?: customs.firstOrNull { it.isLocal } ?: builtIn
}
