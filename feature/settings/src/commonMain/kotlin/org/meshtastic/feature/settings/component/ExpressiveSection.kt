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
package org.meshtastic.feature.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.collapsed
import org.meshtastic.core.resources.expanded
import org.meshtastic.core.ui.icon.ExpandLess
import org.meshtastic.core.ui.icon.ExpandMore
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * A styled section container for settings screens.
 *
 * With [collapsible] (NetworkTact, A3: the main Settings screen), the title is a header that opens and closes the
 * section, [initiallyExpanded] the first time; the choice survives a rotation or a return to the screen. A closed
 * section composes nothing of its content.
 */
@Composable
fun ExpressiveSection(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val open = !collapsible || expanded
    val stateText = stringResource(if (expanded) Res.string.expanded else Res.string.collapsed)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (collapsible) {
            Row(
                modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = title) { expanded = !expanded }
                    .semantics { stateDescription = stateText }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(title = title, color = titleColor, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) MeshtasticIcons.ExpandLess else MeshtasticIcons.ExpandMore,
                    contentDescription = null,
                    tint = titleColor,
                )
            }
        } else {
            SectionTitle(title = title, color = titleColor, modifier = Modifier.padding(horizontal = 16.dp))
        }
        AnimatedVisibility(visible = open) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                content = content,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMediumEmphasized,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}
