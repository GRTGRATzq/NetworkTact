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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.a11y_node_offline
import org.meshtastic.core.ui.theme.AppTheme

/**
 * The APP-6(C) "friend" frame for a land unit: a rectangle with a black edge and the friendly crystal-blue fill, with
 * the radio's short name inside and nothing else. Every radio of the network is drawn with it, wherever it appears.
 *
 * The drawing itself is [drawFriendlyFrame], so the map can rasterize exactly the same pixels into its marker images.
 * - **Offline**: dashed edge (APP-6's mark for a status that is assumed rather than known) and a paler fill.
 * - **Selected**: an orange ring around the frame, outside it, so the frame itself never changes.
 * - **Ignored** ([struckThrough]): the name is struck through.
 *
 * The space for the ring is always reserved, so selecting a radio never moves anything around it.
 */
@Composable
fun FriendlyUnitFrame(
    shortName: String,
    modifier: Modifier = Modifier,
    offline: Boolean = false,
    selected: Boolean = false,
    struckThrough: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = MaterialTheme.typography.labelLarge
    val geometry = remember(density) { FriendlyFrameGeometry(density) }
    val label = friendlyFrameLabel(shortName)
    val layout =
        remember(label, struckThrough, baseStyle, geometry) {
            measureFriendlyLabel(measurer, label, struckThrough, baseStyle, density, geometry)
        }
    val offlineText = stringResource(Res.string.a11y_node_offline)
    val clickModifier = onClick?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier

    Canvas(
        modifier =
        modifier.size(FriendlyFrame.OuterSize).then(clickModifier).semantics {
            contentDescription = label
            if (offline) stateDescription = offlineText
            this.selected = selected
        },
    ) {
        drawFriendlyFrame(label = layout, offline = offline, selected = selected, geometry = geometry)
    }
}

/** The fixed look of the friend frame, shared by the Compose component and the map's marker images. */
object FriendlyFrame {
    /** APP-6 friendly fill, "crystal" blue. */
    val Fill = Color(0xFF80E0FF)

    /** The same blue half-way to white: still friendly, visibly not current. */
    val OfflineFill = Color(0xFFC0F0FF)
    val Edge = Color.Black
    val Label = Color.Black

    /** A thin light rim outside the black edge, so the frame keeps its outline on a dark map or a dark theme. */
    val Halo = Color.White

    /** The map's highlight orange. */
    val SelectionRing = Color(0xFFFF8C00)

    /** 3:2, the proportions of APP-6's land-unit frame. */
    val FrameWidth = 48.dp
    val FrameHeight = 32.dp
    val EdgeWidth = 2.dp
    val HaloWidth = 1.dp
    val SelectionWidth = 3.dp

    /** Offline dashes; [dashRuns] spreads them so every side starts and ends on a dash. */
    val DashLength = 5.dp
    val DashGap = 3.dp

    /** Room between the edge and the name. */
    val LabelPadding = 3.dp

    /** The frame plus its halo and the reserved selection ring on every side. */
    val OuterSize: DpSize =
        DpSize(FrameWidth + (HaloWidth + SelectionWidth) * 2, FrameHeight + (HaloWidth + SelectionWidth) * 2)

    /** Tried in order until the name fits: four wide capitals do not fit at the first size. */
    val LabelSizes: List<TextUnit> = listOf(14.sp, 13.sp, 12.sp, 11.sp, 10.sp)

    const val MAX_LABEL_CHARS = 4
    const val UNNAMED_LABEL = "???"
}

/**
 * What goes in the frame: the short name, trimmed, at most [FriendlyFrame.MAX_LABEL_CHARS] characters (a surrogate pair
 * counts as one), or [FriendlyFrame.UNNAMED_LABEL] for a radio that has not sent a name yet.
 */
fun friendlyFrameLabel(shortName: String): String {
    val trimmed = shortName.trim()
    if (trimmed.isEmpty()) return FriendlyFrame.UNNAMED_LABEL
    var end = 0
    var count = 0
    while (end < trimmed.length && count < FriendlyFrame.MAX_LABEL_CHARS) {
        end += if (trimmed[end].isHighSurrogate() && end + 1 < trimmed.length) 2 else 1
        count++
    }
    return trimmed.substring(0, end)
}

/**
 * Where [length] pixels of a dashed side are inked: dashes of [dash] pixels, at least [gap] pixels apart, the first
 * starting at 0 and the last ending at [length], so the corners are always solid and the frame keeps its shape. The
 * pixels left over are spread one by one over the gaps, so every dash is exactly [dash] wide and nothing is blurred by
 * a fractional edge.
 */
fun dashRuns(length: Int, dash: Int, gap: Int): List<IntRange> = when {
    length <= 0 -> emptyList()

    dash <= 0 || length < dash * 2 + gap -> listOf(0 until length)

    else -> {
        val count = (length + gap) / (dash + gap)
        val gaps = count - 1
        val totalGap = length - count * dash
        var start = 0
        List(count) { index ->
            val run = start until start + dash
            start += dash + totalGap / gaps + if (index < totalGap % gaps) 1 else 0
            run
        }
    }
}

/**
 * The frame's measurements in whole device pixels. Every edge lands on a pixel boundary, so the black edge and the
 * dashes are drawn without anti-aliasing at any density.
 */
class FriendlyFrameGeometry(density: Density) {
    val edgePx: Int = with(density) { FriendlyFrame.EdgeWidth.roundToPx() }.coerceAtLeast(MIN_EDGE_PX)
    val haloPx: Int = with(density) { FriendlyFrame.HaloWidth.roundToPx() }.coerceAtLeast(1)
    val ringPx: Int = with(density) { FriendlyFrame.SelectionWidth.roundToPx() }.coerceAtLeast(MIN_RING_PX)
    val frameWidthPx: Int = with(density) { FriendlyFrame.FrameWidth.roundToPx() }
    val frameHeightPx: Int = with(density) { FriendlyFrame.FrameHeight.roundToPx() }

    /** Dashes never thinner than the edge, gaps at least one pixel. */
    val dashPx: Int = with(density) { FriendlyFrame.DashLength.roundToPx() }.coerceAtLeast(edgePx)
    val gapPx: Int = with(density) { FriendlyFrame.DashGap.roundToPx() }.coerceAtLeast(1)

    /** Where the frame starts, inside the ring and the halo. */
    val framePx: Int = ringPx + haloPx
    val outerWidthPx: Int = frameWidthPx + framePx * 2
    val outerHeightPx: Int = frameHeightPx + framePx * 2
    val labelMaxWidthPx: Int = frameWidthPx - 2 * (edgePx + with(density) { FriendlyFrame.LabelPadding.roundToPx() })
}

/** Lays the name out at the largest of [FriendlyFrame.LabelSizes] that fits the frame. */
fun measureFriendlyLabel(
    measurer: TextMeasurer,
    label: String,
    struckThrough: Boolean,
    baseStyle: TextStyle,
    density: Density,
    geometry: FriendlyFrameGeometry,
): TextLayoutResult {
    val maxWidth = geometry.labelMaxWidthPx.coerceAtLeast(1)
    fun measure(size: TextUnit, constraints: Constraints = Constraints()) = measurer.measure(
        text = AnnotatedString(label),
        style =
        baseStyle.copy(
            color = FriendlyFrame.Label,
            fontSize = size,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.LineThrough.takeIf { struckThrough },
        ),
        overflow = TextOverflow.Clip,
        softWrap = false,
        maxLines = 1,
        constraints = constraints,
        density = density,
    )
    return FriendlyFrame.LabelSizes.firstNotNullOfOrNull { size -> measure(size).takeIf { it.size.width <= maxWidth } }
        ?: measure(FriendlyFrame.LabelSizes.last(), Constraints(maxWidth = maxWidth))
}

/**
 * Draws the frame with its top-left outer corner at ([left], [top]), in an area of [FriendlyFrameGeometry.outerWidthPx]
 * × [FriendlyFrameGeometry.outerHeightPx]: selection ring, halo, fill, edge (solid or dashed), then the name centred.
 */
fun DrawScope.drawFriendlyFrame(
    label: TextLayoutResult,
    offline: Boolean,
    selected: Boolean,
    geometry: FriendlyFrameGeometry,
    left: Float = 0f,
    top: Float = 0f,
) {
    val g = geometry
    if (selected) {
        drawPixelRect(FriendlyFrame.SelectionRing, left, top, g.outerWidthPx, g.outerHeightPx)
    }
    drawPixelRect(
        FriendlyFrame.Halo,
        left + g.ringPx,
        top + g.ringPx,
        g.frameWidthPx + g.haloPx * 2,
        g.frameHeightPx + g.haloPx * 2,
    )
    val frameLeft = left + g.framePx
    val frameTop = top + g.framePx
    drawPixelRect(
        if (offline) FriendlyFrame.OfflineFill else FriendlyFrame.Fill,
        frameLeft,
        frameTop,
        g.frameWidthPx,
        g.frameHeightPx,
    )
    drawEdge(frameLeft, frameTop, g, dashed = offline)
    drawText(
        textLayoutResult = label,
        topLeft =
        Offset(
            x = frameLeft + (g.frameWidthPx - label.size.width) / 2,
            y = frameTop + (g.frameHeightPx - label.size.height) / 2,
        ),
    )
}

/** The black edge as filled pixel rectangles rather than a stroke, which would straddle pixel boundaries. */
private fun DrawScope.drawEdge(frameLeft: Float, frameTop: Float, g: FriendlyFrameGeometry, dashed: Boolean) {
    val e = g.edgePx
    val horizontal = if (dashed) dashRuns(g.frameWidthPx, g.dashPx, g.gapPx) else listOf(0 until g.frameWidthPx)
    val vertical = if (dashed) dashRuns(g.frameHeightPx, g.dashPx, g.gapPx) else listOf(0 until g.frameHeightPx)
    horizontal.forEach { run ->
        val width = run.last - run.first + 1
        drawPixelRect(FriendlyFrame.Edge, frameLeft + run.first, frameTop, width, e)
        drawPixelRect(FriendlyFrame.Edge, frameLeft + run.first, frameTop + g.frameHeightPx - e, width, e)
    }
    vertical.forEach { run ->
        val height = run.last - run.first + 1
        drawPixelRect(FriendlyFrame.Edge, frameLeft, frameTop + run.first, e, height)
        drawPixelRect(FriendlyFrame.Edge, frameLeft + g.frameWidthPx - e, frameTop + run.first, e, height)
    }
}

private fun DrawScope.drawPixelRect(color: Color, left: Float, top: Float, width: Int, height: Int) {
    drawRect(color = color, topLeft = Offset(left, top), size = Size(width.toFloat(), height.toFloat()))
}

private const val MIN_EDGE_PX = 2
private const val MIN_RING_PX = 2

@PreviewLightDark
@Composable
private fun FriendlyUnitFramePreview() {
    AppTheme {
        Row {
            FriendlyUnitFrame(shortName = "ALFA")
            FriendlyUnitFrame(shortName = "MMMM", selected = true)
            FriendlyUnitFrame(shortName = "DRON", offline = true)
            FriendlyUnitFrame(shortName = "", struckThrough = true)
        }
    }
}
