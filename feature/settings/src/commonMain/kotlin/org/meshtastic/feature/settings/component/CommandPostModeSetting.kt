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
package org.meshtastic.feature.settings.component

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.command_post_mode
import org.meshtastic.core.resources.command_post_mode_summary
import org.meshtastic.core.ui.component.SwitchPreference

/** Display mode switch: off keeps the field (Terrain) layout, on opens the command post (PC) view on launch. */
@Composable
internal fun CommandPostModeSetting(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SwitchPreference(
        title = stringResource(Res.string.command_post_mode),
        summary = stringResource(Res.string.command_post_mode_summary),
        checked = checked,
        enabled = true,
        onCheckedChange = onCheckedChange,
    )
}
