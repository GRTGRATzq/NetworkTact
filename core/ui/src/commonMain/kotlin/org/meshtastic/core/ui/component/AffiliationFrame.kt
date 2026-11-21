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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.geo.WaypointAffiliation
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.waypoint_affiliation_friend
import org.meshtastic.core.resources.waypoint_affiliation_hostile
import org.meshtastic.core.resources.waypoint_affiliation_neutral
import org.meshtastic.core.resources.waypoint_affiliation_unknown
import org.meshtastic.core.ui.theme.AppTheme
import kotlin.math.sqrt

/**
 * A waypoint's APP-6(C) affiliation frame, empty: Friend is a 3:2 rectangle, Hostile a diamond, Neutral a square,
 * Unknown a four-lobed quatrefoil, each with a black edge and a thin white rim. A waypoint is not a radio, so the frame
 * is smaller than [FriendlyUnitFrame] and holds no text; its name is written beside it.
 *
 * The drawing itself is [drawAffiliationFrame], so the map can rasterize exactly the same pixels into its marker
 * images. The room for the orange selection ring is always reserved, so selecting a waypoint never moves anything
 * around it.
 */
@Composable
fun AffiliationFrame(affiliation: WaypointAffiliation, modifier: Modifier = Modifier, selected: Boolean = false) {
    val density = LocalDensity.current
    val geometry = remember(density, affiliation) { AffiliationFrameGeometry(density, affiliation) }
    val label = stringResource(affiliationLabel(affiliation))
    Canvas(
        modifier = modifier.size(AffiliationFrames.outerSize(affiliation)).semantics { contentDescription = label },
    ) {
        drawAffiliationFrame(geometry = geometry, selected = selected)
    }
}

/**
 * The four affiliations as a single choice, in place of the old emoji list: each option is its frame with its name
 * under it, and the chosen one is ringed.
 */
@Composable
fun AffiliationSelector(
    selected: WaypointAffiliation,
    onSelect: (WaypointAffiliation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        AffiliationFrames.SelectorOrder.forEach { affiliation ->
            val isSelected = affiliation == selected
            Column(
                modifier =
                Modifier.weight(1f)
                    .padding(horizontal = 2.dp)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(affiliation) })
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The tallest frame sets the height, so the four names line up.
                Column(
                    modifier = Modifier.size(AffiliationFrames.SelectorCellSize),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AffiliationFrame(affiliation = affiliation, selected = isSelected)
                }
                Text(
                    text = stringResource(affiliationLabel(affiliation)),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The name of [affiliation], as shown under its frame and read by TalkBack. */
fun affiliationLabel(affiliation: WaypointAffiliation): StringResource = when (affiliation) {
    WaypointAffiliation.FRIEND -> Res.string.waypoint_affiliation_friend
    WaypointAffiliation.HOSTILE -> Res.string.waypoint_affiliation_hostile
    WaypointAffiliation.NEUTRAL -> Res.string.waypoint_affiliation_neutral
    WaypointAffiliation.UNKNOWN -> Res.string.waypoint_affiliation_unknown
}

/**
 * Every colour and size of the four frames, in one place so they can be tuned on the phone.
 *
 * The Hostile, Neutral and Unknown fills are to be confirmed against APP-6(C) before this is merged.
 */
object AffiliationFrames {
    // Colours.
    val FriendFill: Color = FriendlyFrame.Fill
    val HostileFill = Color(0xFFFF8080)
    val NeutralFill = Color(0xFFAAFFAA)
    val UnknownFill = Color(0xFFFFFF80)
    val Edge: Color = FriendlyFrame.Edge
    val Halo: Color = FriendlyFrame.Halo
    val SelectionRing: Color = FriendlyFrame.SelectionRing

    // Sizes of each shape, its black edge included, without the halo and the selection ring. Chosen for a comparable
    // visual surface: the diamond and the quatrefoil overflow their area a little to look as large as the others.
    val FriendSize = DpSize(36.dp, 24.dp)

    /** Tip to tip. */
    val HostileSize = DpSize(40.dp, 40.dp)
    val NeutralSize = DpSize(28.dp, 28.dp)
    val UnknownSize = DpSize(32.dp, 32.dp)

    val EdgeWidth: Dp = 2.dp
    val HaloWidth: Dp = 1.dp
    val SelectionWidth: Dp = 3.dp

    /** Radius of each lobe of the quatrefoil, as a share of its side; the lobes overlap, leaving four notches. */
    const val UNKNOWN_LOBE_RATIO = 0.27f

    /** Left to right in the selector. */
    val SelectorOrder: List<WaypointAffiliation> =
        listOf(
            WaypointAffiliation.FRIEND,
            WaypointAffiliation.HOSTILE,
            WaypointAffiliation.NEUTRAL,
            WaypointAffiliation.UNKNOWN,
        )

    fun fill(affiliation: WaypointAffiliation): Color = when (affiliation) {
        WaypointAffiliation.FRIEND -> FriendFill
        WaypointAffiliation.HOSTILE -> HostileFill
        WaypointAffiliation.NEUTRAL -> NeutralFill
        WaypointAffiliation.UNKNOWN -> UnknownFill
    }

    fun shapeSize(affiliation: WaypointAffiliation): DpSize = when (affiliation) {
        WaypointAffiliation.FRIEND -> FriendSize
        WaypointAffiliation.HOSTILE -> HostileSize
        WaypointAffiliation.NEUTRAL -> NeutralSize
        WaypointAffiliation.UNKNOWN -> UnknownSize
    }

    /** The shape plus its halo and the reserved selection ring on every side: the size of its image. */
    fun outerSize(affiliation: WaypointAffiliation): DpSize {
        val margin = (HaloWidth + SelectionWidth) * 2
        val shape = shapeSize(affiliation)
        return DpSize(shape.width + margin, shape.height + margin)
    }

    /** Room for the largest of the four, so options of the selector line up. */
    val SelectorCellSize: DpSize
        get() {
            val sizes = WaypointAffiliation.entries.map(::outerSize)
            return DpSize(sizes.maxOf { it.width }, sizes.maxOf { it.height })
        }
}

/** The frame's measurements in whole device pixels, so its straight edges land on pixel boundaries. */
class AffiliationFrameGeometry(density: Density, val affiliation: WaypointAffiliation) {
    val edgePx: Int = with(density) { AffiliationFrames.EdgeWidth.roundToPx() }.coerceAtLeast(MIN_EDGE_PX)
    val haloPx: Int = with(density) { AffiliationFrames.HaloWidth.roundToPx() }.coerceAtLeast(1)
    val ringPx: Int = with(density) { AffiliationFrames.SelectionWidth.roundToPx() }.coerceAtLeast(MIN_RING_PX)
    val shapeWidthPx: Int = with(density) { AffiliationFrames.shapeSize(affiliation).width.roundToPx() }
    val shapeHeightPx: Int = with(density) { AffiliationFrames.shapeSize(affiliation).height.roundToPx() }

    /** Where the shape starts, inside the ring and the halo. */
    val shapeOffsetPx: Int = ringPx + haloPx
    val outerWidthPx: Int = shapeWidthPx + shapeOffsetPx * 2
    val outerHeightPx: Int = shapeHeightPx + shapeOffsetPx * 2
}

/**
 * Draws the frame with its top-left outer corner at ([left], [top]), in an area of
 * [AffiliationFrameGeometry.outerWidthPx] × [AffiliationFrameGeometry.outerHeightPx]: selection ring, halo, fill, then
 * the black edge. Ring and halo are strokes of the edge's own outline, widened, so they follow every shape.
 */
fun DrawScope.drawAffiliationFrame(
    geometry: AffiliationFrameGeometry,
    selected: Boolean,
    left: Float = 0f,
    top: Float = 0f,
) {
    val g = geometry
    val outline = affiliationOutline(g, left + g.shapeOffsetPx, top + g.shapeOffsetPx)
    // Square corners stay square around the rectangle and the square; the diamond's ring and halo are rounded at the
    // tips, where a mitre would run out of the image.
    val outerJoin = if (g.affiliation.hasSquareCorners()) StrokeJoin.Miter else StrokeJoin.Round
    val edgeJoin = if (g.affiliation == WaypointAffiliation.UNKNOWN) StrokeJoin.Round else StrokeJoin.Miter
    if (selected) {
        drawPath(
            path = outline,
            color = AffiliationFrames.SelectionRing,
            style = Stroke(width = (g.edgePx + 2 * (g.haloPx + g.ringPx)).toFloat(), join = outerJoin),
        )
    }
    drawPath(
        path = outline,
        color = AffiliationFrames.Halo,
        style = Stroke(width = (g.edgePx + 2 * g.haloPx).toFloat(), join = outerJoin),
    )
    drawPath(path = outline, color = AffiliationFrames.fill(g.affiliation))
    drawPath(
        path = outline,
        color = AffiliationFrames.Edge,
        style = Stroke(width = g.edgePx.toFloat(), join = edgeJoin),
    )
}

private fun WaypointAffiliation.hasSquareCorners() =
    this == WaypointAffiliation.FRIEND || this == WaypointAffiliation.NEUTRAL

/**
 * The centre line of the black edge, inset by half its width so the edge stays inside the shape's size: the outer side
 * of the edge, and at the diamond's tips the point of its mitre, touch the size exactly.
 */
private fun affiliationOutline(g: AffiliationFrameGeometry, left: Float, top: Float): Path {
    val half = g.edgePx / 2f
    val width = g.shapeWidthPx.toFloat()
    val height = g.shapeHeightPx.toFloat()
    return when (g.affiliation) {
        WaypointAffiliation.FRIEND,
        WaypointAffiliation.NEUTRAL,
        ->
            Path().apply { addRect(Rect(left + half, top + half, left + width - half, top + height - half)) }

        WaypointAffiliation.HOSTILE -> {
            // A mitre at a right angle reaches half the edge times √2 past the centre line.
            val tip = half * sqrt(2f)
            val cx = left + width / 2
            val cy = top + height / 2
            Path().apply {
                moveTo(cx, top + tip)
                lineTo(left + width - tip, cy)
                lineTo(cx, top + height - tip)
                lineTo(left + tip, cy)
                close()
            }
        }

        WaypointAffiliation.UNKNOWN -> {
            val side = minOf(width, height)
            val lobe = side * AffiliationFrames.UNKNOWN_LOBE_RATIO
            val reach = side / 2 - lobe
            val radius = lobe - half
            val centre = Offset(left + width / 2, top + height / 2)
            listOf(Offset(0f, -reach), Offset(reach, 0f), Offset(0f, reach), Offset(-reach, 0f))
                .map { offset -> Path().apply { addOval(Rect(center = centre + offset, radius = radius)) } }
                .reduce { union, lobePath -> Path.combine(PathOperation.Union, union, lobePath) }
        }
    }
}

private const val MIN_EDGE_PX = 2
private const val MIN_RING_PX = 2

@PreviewLightDark
@Composable
private fun AffiliationFramePreview() {
    AppTheme {
        Column {
            Row {
                WaypointAffiliation.entries.forEach { AffiliationFrame(it) }
                AffiliationFrame(WaypointAffiliation.HOSTILE, selected = true)
            }
            AffiliationSelector(selected = WaypointAffiliation.UNKNOWN, onSelect = {})
        }
    }
}
