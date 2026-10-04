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

import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.repository.DemoMode
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

/** The screens' [RadioConfigRepository]: [real] while demo mode is off, [demo] while it is on. */
@Suppress("TooManyFunctions") // Tous les membres sont imposés par l'interface.
class ScreenRadioConfigRepository(
    private val real: RadioConfigRepository,
    private val demo: RadioConfigRepository,
    demoMode: DemoMode,
) : RadioConfigRepository {
    private val switch = ScreenSwitch(demoMode)

    private fun current(): RadioConfigRepository = switch.pick({ real }, { demo })

    override val channelSetFlow: Flow<ChannelSet> = switch.flow({ real.channelSetFlow }, { demo.channelSetFlow })

    override suspend fun clearChannelSet() = current().clearChannelSet()

    override suspend fun replaceAllSettings(settingsList: List<ChannelSettings>) =
        current().replaceAllSettings(settingsList)

    override suspend fun updateChannelSet(settingsList: List<ChannelSettings>?, loraConfig: Config.LoRaConfig?) =
        current().updateChannelSet(settingsList, loraConfig)

    override suspend fun reconcileConversations() = current().reconcileConversations()

    override suspend fun updateChannelSettings(channel: Channel) = current().updateChannelSettings(channel)

    override val localConfigFlow: Flow<LocalConfig> = switch.flow({ real.localConfigFlow }, { demo.localConfigFlow })

    override suspend fun clearLocalConfig() = current().clearLocalConfig()

    override suspend fun setLocalConfig(config: Config) = current().setLocalConfig(config)

    override val moduleConfigFlow: Flow<LocalModuleConfig> =
        switch.flow({ real.moduleConfigFlow }, { demo.moduleConfigFlow })

    override suspend fun clearLocalModuleConfig() = current().clearLocalModuleConfig()

    override suspend fun setLocalModuleConfig(config: ModuleConfig) = current().setLocalModuleConfig(config)

    override val deviceProfileFlow: Flow<DeviceProfile> =
        switch.flow({ real.deviceProfileFlow }, { demo.deviceProfileFlow })

    override val deviceUIConfigFlow: Flow<DeviceUIConfig?> =
        switch.flow({ real.deviceUIConfigFlow }, { demo.deviceUIConfigFlow })

    override suspend fun setDeviceUIConfig(config: DeviceUIConfig) = current().setDeviceUIConfig(config)

    override suspend fun clearDeviceUIConfig() = current().clearDeviceUIConfig()

    override val fileManifestFlow: Flow<List<FileInfo>> =
        switch.flow({ real.fileManifestFlow }, { demo.fileManifestFlow })

    override suspend fun addFileInfo(info: FileInfo) = current().addFileInfo(info)

    override suspend fun clearFileManifest() = current().clearFileManifest()

    override val loraRegionPresetMapFlow: Flow<LoRaRegionPresetMap?> =
        switch.flow({ real.loraRegionPresetMapFlow }, { demo.loraRegionPresetMapFlow })

    override suspend fun setLoraRegionPresetMap(map: LoRaRegionPresetMap) = current().setLoraRegionPresetMap(map)

    override suspend fun clearLoraRegionPresetMap() = current().clearLoraRegionPresetMap()
}
