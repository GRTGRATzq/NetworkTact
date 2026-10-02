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
package org.meshtastic.core.prefs.team

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
import org.meshtastic.core.model.team.TeamRosterRecord
import org.meshtastic.core.prefs.di.asUiDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class TeamRosterPrefsImplTest {
    private lateinit var tmpDir: Path
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: TeamRosterPrefsImpl
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    private val manual = TeamRosterRecord(listOf("Alpha", "Bravo"), TeamRosterRecord.Source.MANUAL, timeSeconds = 100)
    private val received =
        TeamRosterRecord(
            listOf("Alpha", "Bravo", "Charlie"),
            TeamRosterRecord.Source.RADIO,
            senderNum = 0x1234abcd,
            senderName = "PC-0",
            timeSeconds = 200,
        )

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
        tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "teamRosterPrefsTest-${Uuid.random()}"
        FileSystem.SYSTEM.createDirectories(tmpDir)
        dataStore =
            PreferenceDataStoreFactory.createWithPath(
                scope = testScope,
                produceFile = { tmpDir / "test.preferences_pb" },
            )
        val dispatchers = CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher)
        prefs = TeamRosterPrefsImpl(dataStore.asUiDataStore(), dispatchers)
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
        FileSystem.SYSTEM.deleteRecursively(tmpDir)
    }

    @Test
    fun `no list until one is adopted`() = testScope.runTest {
        assertNull(prefs.roster.value)
        assertNull(prefs.pendingRoster.value)
    }

    @Test
    fun `adopted list persists with its origin`() = testScope.runTest {
        prefs.adopt(manual)

        dataStore.data.first { it[TeamRosterPrefsImpl.KEY_ROSTER] != null }
        assertEquals(manual, prefs.roster.value)
    }

    @Test
    fun `received list waits for confirmation`() = testScope.runTest {
        prefs.adopt(manual)
        prefs.offerReceived(received)

        assertEquals(manual, prefs.roster.value)
        assertEquals(received, prefs.pendingRoster.value)
    }

    @Test
    fun `received list identical to the adopted one asks nothing`() = testScope.runTest {
        prefs.adopt(manual)
        prefs.offerReceived(received.copy(teams = manual.teams))

        assertNull(prefs.pendingRoster.value)
    }

    @Test
    fun `accepting adopts the pending list`() = testScope.runTest {
        prefs.adopt(manual)
        prefs.offerReceived(received)
        prefs.acceptPending(received)

        assertEquals(received, prefs.roster.value)
        assertNull(prefs.pendingRoster.value)
    }

    @Test
    fun `a newer list is not accepted in place of the one shown`() = testScope.runTest {
        val newer = received.copy(teams = listOf("Delta"), timeSeconds = 300)
        prefs.offerReceived(received)
        prefs.offerReceived(newer)
        prefs.acceptPending(received)

        assertNull(prefs.roster.value)
        assertEquals(newer, prefs.pendingRoster.value)
    }

    @Test
    fun `dismissing drops the pending list only`() = testScope.runTest {
        prefs.adopt(manual)
        prefs.offerReceived(received)
        prefs.dismissPending(received)

        assertEquals(manual, prefs.roster.value)
        assertNull(prefs.pendingRoster.value)
    }

    @Test
    fun `an unreadable stored value reads as no list`() = testScope.runTest {
        dataStore.edit { it[TeamRosterPrefsImpl.KEY_ROSTER] = "{not json" }

        assertNull(prefs.roster.value)
    }
}
