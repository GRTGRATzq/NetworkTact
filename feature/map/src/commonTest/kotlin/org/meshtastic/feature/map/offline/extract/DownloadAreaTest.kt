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
package org.meshtastic.feature.map.offline.extract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadAreaTest {

    @Test
    fun theInitialFrameLeavesATenthOfTheMapOnEverySide() {
        assertEquals(DownloadFrame(40f, 80f, 360f, 720f), DownloadFrame.initial(400f, 800f))
        assertEquals(DownloadFrame(0f, 0f, 400f, 800f), DownloadFrame.wholeView(400f, 800f))
    }

    @Test
    fun aCornerMovesOnlyItsTwoSides() {
        val frame = DownloadFrame(40f, 80f, 360f, 720f)
        assertEquals(
            DownloadFrame(30f, 100f, 360f, 720f),
            frame.dragCorner(FrameCorner.TopLeft, -10f, 20f, 400f, 800f, 48f),
        )
        assertEquals(
            DownloadFrame(40f, 80f, 380f, 700f),
            frame.dragCorner(FrameCorner.BottomRight, 20f, -20f, 400f, 800f, 48f),
        )
        assertEquals(
            DownloadFrame(40f, 60f, 350f, 720f),
            frame.dragCorner(FrameCorner.TopRight, -10f, -20f, 400f, 800f, 48f),
        )
        assertEquals(
            DownloadFrame(50f, 80f, 360f, 730f),
            frame.dragCorner(FrameCorner.BottomLeft, 10f, 10f, 400f, 800f, 48f),
        )
    }

    @Test
    fun theFrameStaysOnTheMapAndNeverTurnsOver() {
        val frame = DownloadFrame(40f, 80f, 360f, 720f)
        assertEquals(
            DownloadFrame(0f, 0f, 360f, 720f),
            frame.dragCorner(FrameCorner.TopLeft, -500f, -500f, 400f, 800f, 48f),
        )
        assertEquals(
            DownloadFrame(312f, 672f, 360f, 720f),
            frame.dragCorner(FrameCorner.TopLeft, 900f, 900f, 400f, 800f, 48f),
        )
        assertEquals(
            DownloadFrame(40f, 80f, 400f, 800f),
            frame.dragCorner(FrameCorner.BottomRight, 900f, 900f, 400f, 800f, 48f),
        )
    }

    @Test
    fun boundsComeFromTheTwoProjectedCorners() {
        val bounds = DownloadArea.bounds(24.4126 to 46.1865, 25.1874 to 45.6475)
        assertEquals(GeoBounds(24.4126, 45.6475, 25.1874, 46.1865), bounds)
        assertNull(DownloadArea.bounds(null, 25.0 to 45.0))
        assertNull(DownloadArea.bounds(170.0 to 10.0, -170.0 to -10.0), "antimeridian")
        assertNull(DownloadArea.bounds(181.0 to 10.0, 185.0 to -10.0), "a copy of the world beyond 180°")
    }

    @Test
    fun theCincuRectangleIsSixtyByFortyFiveKilometres() {
        val cincu = GeoBounds(24.4126, 45.6475, 25.1874, 46.1865)
        assertEquals(60.0, DownloadArea.widthMeters(cincu) / 1000, 0.5)
        assertEquals(60.0, DownloadArea.heightMeters(cincu) / 1000, 0.5)
        assertEquals(AreaCheck.Ok, DownloadArea.check(cincu))
    }

    @Test
    fun aTinyAreaIsRefused() {
        assertEquals(AreaCheck.TooSmall, DownloadArea.check(GeoBounds(24.8, 45.9, 24.801, 45.91)))
        assertEquals(AreaCheck.Invalid, DownloadArea.check(null))
    }

    @Test
    fun oneDegreeOfLatitudeIsAbout111Kilometres() {
        assertEquals(111.2, DownloadArea.distanceMeters(45.0, 24.0, 46.0, 24.0) / 1000, 0.1)
    }
}
