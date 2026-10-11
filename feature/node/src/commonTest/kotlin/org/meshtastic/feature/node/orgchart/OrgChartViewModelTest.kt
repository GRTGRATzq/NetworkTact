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
package org.meshtastic.feature.node.orgchart

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.orgchart.EditResult
import org.meshtastic.core.model.orgchart.OrgChart
import org.meshtastic.core.model.orgchart.OrgChartCode
import org.meshtastic.core.model.orgchart.OrgChartEdit
import org.meshtastic.core.model.orgchart.OrgChartRecord
import org.meshtastic.core.model.orgchart.OrgMember
import org.meshtastic.core.model.orgchart.OrgTeam
import org.meshtastic.core.testing.FakeNodeRepository
import org.meshtastic.core.testing.FakeOrgChartPrefs
import org.meshtastic.core.testing.FakeTeamRosterPrefs
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.proto.User
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class OrgChartViewModelTest {

    private val nodes = FakeNodeRepository()
    private val prefs = FakeOrgChartPrefs()
    private val uiPrefs = FakeUiPrefs()
    private lateinit var viewModel: OrgChartViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        nodes.setOurNode(Node(num = 100, user = User.Builder().also { it.long_name = "PC-0 [Alpha]" }.build()))
        viewModel = OrgChartViewModel(nodes, prefs, FakeTeamRosterPrefs(), uiPrefs)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun outsideCommandPostModeNothingIsChanged() {
        uiPrefs.setCommandPostMode(false)
        assertEquals(EditResult.Unchanged, viewModel.edit(OrgChartEdit.Add(null, "PC-0", "Chef PC")))
        assertNull(prefs.current.value)
    }

    @Test
    fun theCommandPostSavesItsChangeSignedWithItsCallSign() {
        uiPrefs.setCommandPostMode(true)
        assertIs<EditResult.Changed>(viewModel.edit(OrgChartEdit.Add("Alpha", "ALPHA-1", "Chef d'équipe")))
        val saved = prefs.current.value
        assertEquals(OrgChartRecord.Source.EDITED, saved?.source)
        assertEquals("PC-0", saved?.chart?.author)
        assertEquals(listOf(OrgMember("ALPHA-1", "Chef d'équipe")), saved?.chart?.membersOf("Alpha"))
    }

    @Test
    fun aRefusedChangeSavesNothing() {
        uiPrefs.setCommandPostMode(true)
        viewModel.edit(OrgChartEdit.Add(null, "PC-0", ""))
        val before = prefs.current.value
        assertIs<EditResult.Invalid>(viewModel.edit(OrgChartEdit.Add("Alpha", "pc-0", "")))
        assertEquals(before, prefs.current.value)
    }

    private val scanned =
        OrgChart(
            "PC-1",
            1_790_000_000L,
            listOf(OrgMember("PC-1", "Chef PC")),
            listOf(OrgTeam("Alpha", listOf(OrgMember("ALPHA-1")))),
        )

    @Test
    fun aScannedChartIsOnlyOfferedThenAdoptedKeepingThePreviousOne() {
        uiPrefs.setCommandPostMode(true)
        viewModel.edit(OrgChartEdit.Add(null, "PC-0", ""))
        val mine = prefs.current.value

        viewModel.onScanned(OrgChartCode.encode(scanned) + "\n")
        assertEquals(ScanOutcome.Found(scanned), viewModel.scan.value)
        assertEquals(mine, prefs.current.value)

        viewModel.adoptScanned(scanned)
        assertEquals(scanned, prefs.current.value?.chart)
        assertEquals(OrgChartRecord.Source.SCANNED, prefs.current.value?.source)
        assertEquals(mine, prefs.previous.value)
        assertEquals(ScanOutcome.Adopted, viewModel.scan.value)
    }

    @Test
    fun anyOtherCodeOrTheSameChartIsReportedNotOffered() {
        viewModel.onScanned("https://meshtastic.org/e/#CgMSAQESBggBQANIAQ")
        assertEquals(ScanOutcome.Invalid, viewModel.scan.value)

        prefs.adopt(OrgChartRecord(scanned, OrgChartRecord.Source.SCANNED, 1L))
        viewModel.onScanned(OrgChartCode.encode(scanned.copy(author = "PC-9", createdSeconds = 5L)))
        assertEquals(ScanOutcome.Same, viewModel.scan.value)

        viewModel.clearScan()
        viewModel.onScanned(null)
        assertNull(viewModel.scan.value)
    }

    @Test
    fun theQrCodeCarriesTheChartInUse() {
        assertEquals(QrCodeState.Empty, viewModel.qrCode())
        prefs.save(OrgChartRecord(scanned, OrgChartRecord.Source.EDITED, 1L))
        val ready = assertIs<QrCodeState.Ready>(viewModel.qrCode())
        assertEquals(scanned, OrgChartCode.decode(ready.code))
    }
}
