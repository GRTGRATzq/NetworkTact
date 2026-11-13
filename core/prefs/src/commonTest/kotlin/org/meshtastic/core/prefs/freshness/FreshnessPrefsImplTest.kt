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
package org.meshtastic.core.prefs.freshness

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.freshness.FreshnessSettings
import org.meshtastic.core.prefs.di.asUiDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

class FreshnessPrefsImplTest {
    private lateinit var tmpDir: Path
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: FreshnessPrefsImpl
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
        tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "freshnessPrefsTest-${Uuid.random()}"
        FileSystem.SYSTEM.createDirectories(tmpDir)
        dataStore =
            PreferenceDataStoreFactory.createWithPath(
                scope = testScope,
                produceFile = { tmpDir / "test.preferences_pb" },
            )
        val dispatchers = CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher)
        prefs = FreshnessPrefsImpl(dataStore.asUiDataStore(), dispatchers)
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
        FileSystem.SYSTEM.deleteRecursively(tmpDir)
    }

    @Test
    fun `the validated defaults apply until something is saved`() = testScope.runTest {
        assertEquals(
            FreshnessSettings(15.minutes, autoScale = true, fixedThreshold = 35.minutes),
            prefs.settings.value,
        )
    }

    @Test
    fun `saved settings persist`() = testScope.runTest {
        val saved = FreshnessSettings(expectedInterval = 1.hours, autoScale = false, fixedThreshold = 2.hours)
        prefs.setSettings(saved)

        dataStore.data.first { it[FreshnessPrefsImpl.KEY_AUTO] != null }
        assertEquals(saved, prefs.settings.value)
    }

    @Test
    fun `a zero or negative duration reads as its default`() = testScope.runTest {
        dataStore.edit {
            it[FreshnessPrefsImpl.KEY_INTERVAL] = 0
            it[FreshnessPrefsImpl.KEY_FIXED] = -60
        }

        assertEquals(FreshnessSettings.Default, prefs.settings.value)
    }
}
