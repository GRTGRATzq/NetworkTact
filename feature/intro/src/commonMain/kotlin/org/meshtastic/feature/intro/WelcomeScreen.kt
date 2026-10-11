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
package org.meshtastic.feature.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.communicate_off_the_grid
import org.meshtastic.core.resources.create_your_own_networks
import org.meshtastic.core.resources.easily_set_up_private_mesh_networks
import org.meshtastic.core.resources.get_started
import org.meshtastic.core.resources.insigne_4e_batterie
import org.meshtastic.core.resources.insigne_bas
import org.meshtastic.core.resources.intro_insignia_4th_battery
import org.meshtastic.core.resources.intro_insignia_4th_battery_description
import org.meshtastic.core.resources.intro_insignia_acquisition_battery
import org.meshtastic.core.resources.intro_insignia_acquisition_battery_description
import org.meshtastic.core.resources.intro_insignia_joint_project
import org.meshtastic.core.resources.intro_welcome
import org.meshtastic.core.resources.share_your_location_in_real_time
import org.meshtastic.core.resources.stay_connected_anywhere
import org.meshtastic.core.resources.track_and_share_locations
import org.meshtastic.core.ui.component.NetworkTactLogo
import org.meshtastic.core.ui.icon.Antenna
import org.meshtastic.core.ui.icon.MeshHub
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.NearMe
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.LocalAnalyticsIntroProvider

/**
 * The initial welcome screen for the app introduction flow. It displays a brief overview of the app's key features.
 *
 * @param onGetStarted Callback invoked when the user proceeds from the welcome screen.
 */
@Composable
internal fun WelcomeScreen(onGetStarted: () -> Unit) {
    val analyticsIntro = LocalAnalyticsIntroProvider.current
    val features =
        listOf(
            FeatureUIData(
                icon = MeshtasticIcons.Antenna,
                titleRes = Res.string.stay_connected_anywhere,
                subtitleRes = Res.string.communicate_off_the_grid,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.MeshHub,
                titleRes = Res.string.create_your_own_networks,
                subtitleRes = Res.string.easily_set_up_private_mesh_networks,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.NearMe,
                titleRes = Res.string.track_and_share_locations,
                subtitleRes = Res.string.share_your_location_in_real_time,
            ),
        )

    Scaffold(
        bottomBar = {
            IntroBottomBar(
                onSkip = {},
                onConfigure = onGetStarted,
                skipButtonText = "",
                configureButtonText = stringResource(Res.string.get_started),
                showSkipButton = false,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.intro_welcome),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            NetworkTactLogo()
            Spacer(modifier = Modifier.height(32.dp))
            features.forEach { feature ->
                FeatureRow(feature = feature)
                Spacer(modifier = Modifier.height(16.dp))
            }
            BatteryInsigniaBlock()
            Spacer(modifier = Modifier.weight(1f))
            analyticsIntro()
        }
    }
}

/** Same height for both insignia; they keep their original colours in light and dark themes. */
private val InsigniaHeight = 88.dp

/**
 * The insignia of the two batteries carrying the project, side by side with their captions and separated by a thin
 * vertical rule, followed by a centred sentence. The images are never tinted.
 */
@Composable
private fun BatteryInsigniaBlock() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            BatteryInsignia(
                image = Res.drawable.insigne_4e_batterie,
                descriptionRes = Res.string.intro_insignia_4th_battery_description,
                captionRes = Res.string.intro_insignia_4th_battery,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            BatteryInsignia(
                image = Res.drawable.insigne_bas,
                descriptionRes = Res.string.intro_insignia_acquisition_battery_description,
                captionRes = Res.string.intro_insignia_acquisition_battery,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.intro_insignia_joint_project),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BatteryInsignia(
    image: DrawableResource,
    descriptionRes: StringResource,
    captionRes: StringResource,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(image),
            contentDescription = stringResource(descriptionRes),
            modifier = Modifier.height(InsigniaHeight),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(captionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("PreviewPublic")
@PreviewLightDark
@Composable
fun WelcomeScreenPreview() {
    AppTheme { Surface { WelcomeScreen(onGetStarted = {}) } }
}
