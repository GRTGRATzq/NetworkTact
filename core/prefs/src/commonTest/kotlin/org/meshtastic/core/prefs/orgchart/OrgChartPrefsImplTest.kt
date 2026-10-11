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
package org.meshtastic.core.prefs.orgchart

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
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.model.orgchart.OrgMember
import org.meshtastic.core.model.orgchart.OrgTeam
import org.meshtastic.core.prefs.di.asUiDataStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class OrgChartPrefsImplTest {
    private lateinit var tmpDir: Path
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: OrgChartPrefsImpl
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    private val mine =
        OrgChartRecord(
            OrgChart(
                author = "PC-0",
                createdSeconds = 100,
                commandPost = listOf(OrgMember("PC-0", "Chef PC")),
                teams = listOf(OrgTeam("Alpha", listOf(OrgMember("ALPHA-1", "Chef d'équipe")))),
            ),
            OrgChartRecord.Source.EDITED,
            timeSeconds = 100,
        )
    private val scanned =
        OrgChartRecord(
            mine.chart.copy(author = "PC-1", teams = listOf(OrgTeam("Bravo", listOf(OrgMember("BRAVO-2"))))),
            OrgChartRecord.Source.SCANNED,
            timeSeconds = 200,
        )

    @BeforeTest
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
        tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "orgChartPrefsTest-${Uuid.random()}"
        FileSystem.SYSTEM.createDirectories(tmpDir)
        dataStore =
            PreferenceDataStoreFactory.createWithPath(
                scope = testScope,
                produceFile = { tmpDir / "test.preferences_pb" },
            )
        val dispatchers = CoroutineDispatchers(testDispatcher, testDispatcher, testDispatcher)
        prefs = OrgChartPrefsImpl(dataStore.asUiDataStore(), dispatchers)
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
        FileSystem.SYSTEM.deleteRecursively(tmpDir)
    }

    @Test
    fun `no chart until one is saved or adopted`() = testScope.runTest {
        assertNull(prefs.current.value)
        assertNull(prefs.previous.value)
    }

    @Test
    fun `a saved chart persists whole`() = testScope.runTest {
        prefs.save(mine)

        dataStore.data.first { it[OrgChartPrefsImpl.KEY_CURRENT] != null }
        assertEquals(mine, prefs.current.value)
        assertNull(prefs.previous.value)
    }

    @Test
    fun `adopting keeps the replaced chart and going back swaps them`() = testScope.runTest {
        prefs.save(mine)
        prefs.adopt(scanned)

        assertEquals(scanned, prefs.current.value)
        assertEquals(mine, prefs.previous.value)

        prefs.revertToPrevious()

        assertEquals(mine, prefs.current.value)
        assertEquals(scanned, prefs.previous.value)
    }

    @Test
    fun `an unreadable value reads as no chart`() = testScope.runTest {
        dataStore.edit { it[OrgChartPrefsImpl.KEY_CURRENT] = "{not a chart" }

        assertNull(prefs.current.value)
    }
}
