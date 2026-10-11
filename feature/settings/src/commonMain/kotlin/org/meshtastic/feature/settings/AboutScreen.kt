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
package org.meshtastic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.about
import org.meshtastic.core.resources.acknowledgements
import org.meshtastic.core.resources.app_version
import org.meshtastic.core.resources.apps
import org.meshtastic.core.resources.copyright_notice
import org.meshtastic.core.resources.github_repository
import org.meshtastic.core.resources.license
import org.meshtastic.core.resources.license_notice
import org.meshtastic.core.resources.networktact_based_on
import org.meshtastic.core.resources.project_information
import org.meshtastic.core.resources.what_is_meshtastic
import org.meshtastic.core.resources.what_is_meshtastic_description
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.Code
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.Memory
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Notes
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.feature.settings.component.ExpressiveSection

private const val GITHUB_REPO_URL = "https://github.com/GRTGRATzq/NetworkTact"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/**
 * About screen displaying general information about Meshtastic, hardware recommendations, repository and version
 * details, acknowledgements, and project links.
 */
@Composable
fun AboutScreen(
    appVersionName: String,
    onNavigateUp: () -> Unit,
    onNavigateToAcknowledgements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.about),
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                ourNode = null,
                showNodeChip = false,
                onClickChip = {},
                actions = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WhatIsMeshtasticSection()
            AppsSection(
                appVersionName = appVersionName,
                onNavigateToAcknowledgements = onNavigateToAcknowledgements,
                onOpenRepoLink = { uriHandler.openUri(GITHUB_REPO_URL) },
            )
            ProjectInformationSection(onOpenLicense = { uriHandler.openUri(LICENSE_URL) })
            CopyrightFooter()
        }
    }
}

@Composable
private fun WhatIsMeshtasticSection(modifier: Modifier = Modifier) {
    ExpressiveSection(title = stringResource(Res.string.what_is_meshtastic), modifier = modifier) {
        Text(
            text = stringResource(Res.string.what_is_meshtastic_description),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppsSection(
    appVersionName: String,
    onNavigateToAcknowledgements: () -> Unit,
    onOpenRepoLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpressiveSection(title = stringResource(Res.string.apps), modifier = modifier) {
        ListItem(
            text = stringResource(Res.string.github_repository),
            leadingIcon = MeshtasticIcons.Code,
            trailingIcon = MeshtasticIcons.ChevronRight,
            onClick = onOpenRepoLink,
        )
        ListItem(
            text = stringResource(Res.string.app_version),
            leadingIcon = MeshtasticIcons.Memory,
            supportingText = appVersionName,
            trailingIcon = null,
        )
        ListItem(
            text = stringResource(Res.string.acknowledgements),
            leadingIcon = MeshtasticIcons.Info,
            trailingIcon = MeshtasticIcons.ChevronRight,
            onClick = onNavigateToAcknowledgements,
        )
    }
}

@Composable
private fun ProjectInformationSection(onOpenLicense: () -> Unit, modifier: Modifier = Modifier) {
    ExpressiveSection(title = stringResource(Res.string.project_information), modifier = modifier) {
        ListItem(
            text = stringResource(Res.string.license),
            leadingIcon = MeshtasticIcons.Notes,
            trailingIcon = MeshtasticIcons.ChevronRight,
            onClick = onOpenLicense,
        )
    }
}

// GPL section 5(d): this screen shows the copyright notice, the no-warranty statement and where the
// license is, so a modified version has to keep showing them too. Keep all three together here.
@Composable
private fun CopyrightFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.networktact_based_on),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.copyright_notice),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.license_notice),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun AboutScreenPreview() {
    AppTheme { AboutScreen(appVersionName = "2.5.0", onNavigateUp = {}, onNavigateToAcknowledgements = {}) }
}
