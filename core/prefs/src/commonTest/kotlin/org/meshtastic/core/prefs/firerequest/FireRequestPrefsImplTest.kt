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
package org.meshtastic.core.prefs.firerequest

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
import org.meshtastic.core.model.firerequest.Command
import org.meshtastic.core.model.firerequest.FireRequest
import org.meshtastic.core.model.firerequest.GdhZone
import org.meshtastic.core.model.firerequest.Location
import org.meshtastic.core.model.firerequest.LruLine
import org.meshtastic.core.prefs.di.asUiDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class FireRequestPrefsImplTest {
    private lateinit var tmpDir: Path
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: FireRequestPrefsImpl
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    private val request =
        FireRequest(
            origin = "OA",
            number = "AB1234",
            location = Location(g = LruLine("35", "T", "LL", "33000", "90300", "610"), tle = "2"),
            command = Command(whenReady = true),
        )

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
        tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fireRequestPrefsTest-${Uuid.random()}"
        FileSystem.SYSTEM.createDirectories(tmpDir)
        dataStore =
            PreferenceDataStoreFactory.createWithPath(
                scope = testScope,
                produceFile = { tmpDir / "test.preferences_pb" },
            )
        val dispatchers = CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher)
        prefs = FireRequestPrefsImpl(dataStore.asUiDataStore(), dispatchers)
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
        FileSystem.SYSTEM.deleteRecursively(tmpDir)
    }

    @Test
    fun `local time and nothing kept until the form is used`() = testScope.runTest {
        assertEquals(GdhZone.LOCAL, prefs.gdhZone.value)
        assertNull(prefs.draft.value)
        assertNull(prefs.lastSent.value)
    }

    @Test
    fun `the chosen zone is kept`() = testScope.runTest {
        prefs.setGdhZone(GdhZone.ZULU)

        dataStore.data.first { it[FireRequestPrefsImpl.KEY_ZONE] != null }
        assertEquals(GdhZone.ZULU, prefs.gdhZone.value)
    }

    @Test
    fun `the draft persists whole and can be thrown away`() = testScope.runTest {
        prefs.saveDraft(request)

        dataStore.data.first { it[FireRequestPrefsImpl.KEY_DRAFT] != null }
        assertEquals(request, prefs.draft.value)

        prefs.saveDraft(null)
        assertNull(prefs.draft.value)
    }

    @Test
    fun `a request sent becomes the last one and the draft goes`() = testScope.runTest {
        prefs.saveDraft(request)
        prefs.recordSent(request)

        assertEquals(request, prefs.lastSent.value)
        assertNull(prefs.draft.value)
    }

    @Test
    fun `unreadable values read as nothing kept`() = testScope.runTest {
        dataStore.edit {
            it[FireRequestPrefsImpl.KEY_DRAFT] = "{not a request"
            it[FireRequestPrefsImpl.KEY_ZONE] = "MARS"
        }

        assertNull(prefs.draft.value)
        assertEquals(GdhZone.LOCAL, prefs.gdhZone.value)
    }
}
