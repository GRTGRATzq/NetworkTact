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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_networktact
import org.meshtastic.core.resources.ic_networktact_dark
import org.meshtastic.core.resources.meshtastic_app_name
import org.meshtastic.core.ui.theme.AppTheme

// Wordmark greens. The symbol keeps the source colours (#0A2A1B / #61A748); text uses tones that reach the WCAG
// 4.5:1 text contrast on the app background (#F5F6FA light, #1A1B26 dark). The source "tact" green #61A748 only
// reaches 2.73:1 on the light background, hence the darker #3B7A2A.
private val NetworkGreenLight = Color(0xFF0A2A1B) // 14.3:1 on #F5F6FA
private val TactGreenLight = Color(0xFF3B7A2A) // 4.86:1 on #F5F6FA
private val NetworkGreenDark = Color(0xFFB6E2C2) // 11.9:1 on #1A1B26
private val TactGreenDark = Color(0xFF7CC864) // 8.4:1 on #1A1B26

// The wordmark is a logo, not translatable text: it is always written in lowercase in two colours.
private const val WORDMARK_NETWORK = "network"
private const val WORDMARK_TACT = "tact"

private const val DARK_BACKGROUND_LUMINANCE = 0.5f

/**
 * Follows the theme actually applied (including the in-app theme choice and dynamic colour), not the system setting.
 */
@Composable
private fun isDarkBackground(): Boolean = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE

/** The NetworkTact symbol in colour, with lighter greens on a dark background so the dark nodes stay visible. */
@Composable
fun NetworkTactSymbol(
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(Res.string.meshtastic_app_name),
) {
    val drawable = if (isDarkBackground()) Res.drawable.ic_networktact_dark else Res.drawable.ic_networktact
    Image(imageVector = vectorResource(drawable), contentDescription = contentDescription, modifier = modifier)
}

/** The two-colour "networktact" wordmark, drawn as text so it stays sharp and follows the theme. */
@Composable
fun NetworkTactWordmark(modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.headlineLarge) {
    val dark = isDarkBackground()
    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(color = if (dark) NetworkGreenDark else NetworkGreenLight)) { append(WORDMARK_NETWORK) }
        withStyle(SpanStyle(color = if (dark) TactGreenDark else TactGreenLight)) { append(WORDMARK_TACT) }
    }
    Text(text = wordmark, style = style, modifier = modifier)
}

/** Symbol above the wordmark, announced once by screen readers as the application name. */
@Composable
fun NetworkTactLogo(modifier: Modifier = Modifier) {
    val appName = stringResource(Res.string.meshtastic_app_name)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = appName },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NetworkTactSymbol(modifier = Modifier.size(120.dp), contentDescription = null)
        NetworkTactWordmark()
    }
}

@PreviewLightDark
@Composable
private fun NetworkTactLogoPreview() {
    AppTheme { Surface { NetworkTactLogo(modifier = Modifier.padding(16.dp)) } }
}
