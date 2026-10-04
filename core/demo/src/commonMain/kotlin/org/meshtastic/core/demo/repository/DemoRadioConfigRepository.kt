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
package org.meshtastic.core.demo.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.meshtastic.core.demo.store.DemoStore
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.proto.Channel
import org.meshtastic.proto.ChannelSet
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.Config
import org.meshtastic.proto.DeviceProfile
import org.meshtastic.proto.DeviceUIConfig
import org.meshtastic.proto.FileInfo
import org.meshtastic.proto.LoRaRegionPresetMap
import org.meshtastic.proto.LocalConfig
import org.meshtastic.proto.LocalModuleConfig
import org.meshtastic.proto.ModuleConfig

/**
 * [RadioConfigRepository] for the demo: its three channels, and default (empty) radio and module configuration. The
 * radio configuration screens are unavailable in demo mode; any write lands in memory, never on a radio.
 */
@Suppress("TooManyFunctions") // Tous les membres sont imposés par l'interface.
class DemoRadioConfigRepository(private val store: DemoStore) : RadioConfigRepository {
    override val channelSetFlow: Flow<ChannelSet> = store.channelSet

    override suspend fun clearChannelSet() {
        store.channelSet.value = ChannelSet.Builder().build()
    }

    override suspend fun replaceAllSettings(settingsList: List<ChannelSettings>) {
        store.channelSet.update { set -> set.newBuilder().also { it.settings = settingsList }.build() }
    }

    override suspend fun updateChannelSet(settingsList: List<ChannelSettings>?, loraConfig: Config.LoRaConfig?) {
        if (settingsList != null) replaceAllSettings(settingsList)
    }

    override suspend fun reconcileConversations() = Unit

    override suspend fun updateChannelSettings(channel: Channel) = Unit

    override val localConfigFlow: Flow<LocalConfig> = flowOf(LocalConfig.Builder().build())

    override suspend fun clearLocalConfig() = Unit

    override suspend fun setLocalConfig(config: Config) = Unit

    override val moduleConfigFlow: Flow<LocalModuleConfig> = flowOf(LocalModuleConfig.Builder().build())

    override suspend fun clearLocalModuleConfig() = Unit

    override suspend fun setLocalModuleConfig(config: ModuleConfig) = Unit

    override val deviceProfileFlow: Flow<DeviceProfile> = flowOf(DeviceProfile.Builder().build())

    override val deviceUIConfigFlow: Flow<DeviceUIConfig?> = flowOf(null)

    override suspend fun setDeviceUIConfig(config: DeviceUIConfig) = Unit

    override suspend fun clearDeviceUIConfig() = Unit

    override val fileManifestFlow: Flow<List<FileInfo>> = flowOf(emptyList())

    override suspend fun addFileInfo(info: FileInfo) = Unit

    override suspend fun clearFileManifest() = Unit

    override val loraRegionPresetMapFlow: Flow<LoRaRegionPresetMap?> = flowOf(null)

    override suspend fun setLoraRegionPresetMap(map: LoRaRegionPresetMap) = Unit

    override suspend fun clearLoraRegionPresetMap() = Unit
}
