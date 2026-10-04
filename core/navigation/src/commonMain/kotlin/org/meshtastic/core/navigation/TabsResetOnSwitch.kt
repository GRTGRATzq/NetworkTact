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
package org.meshtastic.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Puts every tab of [multiBackstack] back on its root each time a switch (demo mode) changes, on the paths
 * [startPathsWhenOn] gives while it is on. [sync] is idempotent, so several callers can report the same change and the
 * tabs are reset once, by whichever comes first: a link opened just after the switch keeps its navigation.
 */
class TabsResetOnSwitch(
    private val multiBackstack: MultiBackstack,
    private var shownFor: Boolean,
    private val startPathsWhenOn: Map<NavKey, List<NavKey>>,
) {
    fun sync(on: Boolean) {
        if (on == shownFor) return
        shownFor = on
        multiBackstack.resetAllTabs(if (on) startPathsWhenOn else emptyMap())
    }
}
