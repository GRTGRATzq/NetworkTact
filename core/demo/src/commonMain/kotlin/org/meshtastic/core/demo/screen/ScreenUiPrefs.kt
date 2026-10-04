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
package org.meshtastic.core.demo.screen

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.repository.DemoMode
import org.meshtastic.core.repository.NodeFilterPrefs
import org.meshtastic.core.repository.UiPrefs

/**
 * The screens' [UiPrefs]. Every preference reads as the real one; while demo mode is on none is written, except the
 * Terrain/PC choice, which then lives in the demo store so that both views can be shown, and is dropped on exit: the
 * real choice is back as it was.
 */
@Suppress("TooManyFunctions") // Tous les membres sont imposés par l'interface.
class ScreenUiPrefs(private val real: UiPrefs, private val store: DemoStore, demoMode: DemoMode) : UiPrefs {
    private val switch = ScreenSwitch(demoMode)

    override val appIntroCompleted: StateFlow<Boolean>
        get() = real.appIntroCompleted

    override fun setAppIntroCompleted(completed: Boolean) {
        if (!switch.isDemo) real.setAppIntroCompleted(completed)
    }

    override val theme: StateFlow<Int>
        get() = real.theme

    override fun setTheme(value: Int) {
        if (!switch.isDemo) real.setTheme(value)
    }

    override val unitsOverride: StateFlow<Int>
        get() = real.unitsOverride

    override fun setUnitsOverride(value: Int) {
        if (!switch.isDemo) real.setUnitsOverride(value)
    }

    override val locale: StateFlow<String>
        get() = real.locale

    override fun setLocale(languageTag: String) {
        if (!switch.isDemo) real.setLocale(languageTag)
    }

    override val nodeSort: StateFlow<Int>
        get() = real.nodeSort

    override fun setNodeSort(value: Int) {
        if (!switch.isDemo) real.setNodeSort(value)
    }

    override val nodeFilters: StateFlow<NodeFilterPrefs>
        get() = real.nodeFilters

    override fun updateNodeFilters(transform: (NodeFilterPrefs) -> NodeFilterPrefs) {
        if (!switch.isDemo) real.updateNodeFilters(transform)
    }

    override val hasShownNotPairedWarning: StateFlow<Boolean>
        get() = real.hasShownNotPairedWarning

    override fun setHasShownNotPairedWarning(shown: Boolean) {
        if (!switch.isDemo) real.setHasShownNotPairedWarning(shown)
    }

    override val showQuickChat: StateFlow<Boolean>
        get() = real.showQuickChat

    override fun setShowQuickChat(show: Boolean) {
        if (!switch.isDemo) real.setShowQuickChat(show)
    }

    override val showFullMessageTimestamps: StateFlow<Boolean>
        get() = real.showFullMessageTimestamps

    override fun setShowFullMessageTimestamps(show: Boolean) {
        if (!switch.isDemo) real.setShowFullMessageTimestamps(show)
    }

    override val commandPostMode: StateFlow<Boolean> = switch.state(real.commandPostMode, store.commandPostMode)

    override fun setCommandPostMode(enabled: Boolean) {
        if (switch.isDemo) store.commandPostMode.value = enabled else real.setCommandPostMode(enabled)
    }

    override val eventThemeEnabled: StateFlow<Boolean>
        get() = real.eventThemeEnabled

    override fun setEventThemeEnabled(enabled: Boolean) {
        if (!switch.isDemo) real.setEventThemeEnabled(enabled)
    }

    override val bleAutoScan: StateFlow<Boolean>
        get() = real.bleAutoScan

    override fun setBleAutoScan(enabled: Boolean) {
        if (!switch.isDemo) real.setBleAutoScan(enabled)
    }

    override val networkAutoScan: StateFlow<Boolean>
        get() = real.networkAutoScan

    override fun setNetworkAutoScan(enabled: Boolean) {
        if (!switch.isDemo) real.setNetworkAutoScan(enabled)
    }

    override val selectedConnectionTransport: StateFlow<DeviceType?>
        get() = real.selectedConnectionTransport

    override fun setSelectedConnectionTransport(type: DeviceType) {
        if (!switch.isDemo) real.setSelectedConnectionTransport(type)
    }

    override val firmwareUpdateNotificationKeys: StateFlow<Set<String>>
        get() = real.firmwareUpdateNotificationKeys

    override fun recordFirmwareUpdateNotificationKey(key: String) {
        if (!switch.isDemo) real.recordFirmwareUpdateNotificationKey(key)
    }

    override fun shouldProvideNodeLocation(nodeNum: Int): StateFlow<Boolean> = real.shouldProvideNodeLocation(nodeNum)

    override fun setShouldProvideNodeLocation(nodeNum: Int, provide: Boolean) {
        if (!switch.isDemo) real.setShouldProvideNodeLocation(nodeNum, provide)
    }

    override val nodeListDensity: StateFlow<String>
        get() = real.nodeListDensity

    override fun setNodeListDensity(value: String) {
        if (!switch.isDemo) real.setNodeListDensity(value)
    }

    override val shouldShowPower: StateFlow<Boolean>
        get() = real.shouldShowPower

    override fun setShouldShowPower(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowPower(value)
    }

    override val shouldShowLastHeard: StateFlow<Boolean>
        get() = real.shouldShowLastHeard

    override fun setShouldShowLastHeard(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowLastHeard(value)
    }

    override val lastHeardIsRelative: StateFlow<Boolean>
        get() = real.lastHeardIsRelative

    override fun setLastHeardIsRelative(value: Boolean) {
        if (!switch.isDemo) real.setLastHeardIsRelative(value)
    }

    override val shouldShowLocation: StateFlow<Boolean>
        get() = real.shouldShowLocation

    override fun setShouldShowLocation(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowLocation(value)
    }

    override val shouldShowHops: StateFlow<Boolean>
        get() = real.shouldShowHops

    override fun setShouldShowHops(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowHops(value)
    }

    override val shouldShowSignal: StateFlow<Boolean>
        get() = real.shouldShowSignal

    override fun setShouldShowSignal(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowSignal(value)
    }

    override val shouldShowChannel: StateFlow<Boolean>
        get() = real.shouldShowChannel

    override fun setShouldShowChannel(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowChannel(value)
    }

    override val shouldShowRole: StateFlow<Boolean>
        get() = real.shouldShowRole

    override fun setShouldShowRole(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowRole(value)
    }

    override val shouldShowTelemetry: StateFlow<Boolean>
        get() = real.shouldShowTelemetry

    override fun setShouldShowTelemetry(value: Boolean) {
        if (!switch.isDemo) real.setShouldShowTelemetry(value)
    }
}
