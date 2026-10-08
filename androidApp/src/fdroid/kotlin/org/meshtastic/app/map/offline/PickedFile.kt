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
package org.meshtastic.app.map.offline

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import org.meshtastic.core.common.util.safeCatching

/** The size the provider of a picked file declares, or null when it does not say. */
internal fun ContentResolver.declaredSize(uri: Uri): Long? = safeCatching {
    query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst() && column >= 0 && !cursor.isNull(column)) cursor.getLong(column) else null
    }
}
    .getOrNull()
    ?.takeIf { it > 0 }

/** Keeps the picker's read grant for the length of a long copy; false if the provider does not offer that. */
internal fun ContentResolver.holdReadPermission(uri: Uri): Boolean =
    safeCatching { takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess

internal fun ContentResolver.releaseReadPermission(uri: Uri) {
    safeCatching { releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
}
