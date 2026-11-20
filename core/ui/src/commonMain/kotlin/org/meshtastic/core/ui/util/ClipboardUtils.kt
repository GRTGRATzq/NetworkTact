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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry

/**
 * Returns a function that reads the system clipboard as plain text, or null when it is empty or holds no text.
 *
 * Read only on an explicit user action (a "Paste" button): Android shows a "pasted from" notice on every read.
 */
@Composable expect fun rememberReadClipboardText(): suspend () -> String?

/**
 * Creates a platform-appropriate [ClipEntry] for the given text.
 *
 * Set [sensitive] for key material or anything else that should not be surfaced by the OS: on Android it marks the clip
 * so the system does not show the copied content in the paste preview toast, and clipboard-reading tools are asked to
 * treat it as secret. Pass it for private keys and for channel URLs, which carry channel PSKs.
 */
expect fun createClipEntry(text: String, label: String = "", sensitive: Boolean = false): ClipEntry
