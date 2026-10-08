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

import android.content.Context
import org.meshtastic.feature.map.offline.emptyGlyphRange
import org.meshtastic.feature.map.offline.glyphRanges
import java.io.File
import java.io.IOException
import java.net.URLEncoder

/**
 * The style, fonts and sprites an offline map draws with, installed from the APK's `assets/offline-map`
 * (scripts/offline-map-assets.mjs) into app storage.
 *
 * Installed, not read in place: MapLibre reads fonts and sprites through `file://` paths, and an APK asset has none.
 * Done only once a map exists, so a phone with no offline map never gets these files.
 *
 * @property dir The directory holding `fonts/` and `sprites/`, for
 *   [org.meshtastic.feature.map.offline.offlineMapStyle].
 * @property styleTemplates The bundled style documents, by flavor (`light`, `dark`).
 */
class OfflineMapAssets(val dir: File, val styleTemplates: Map<String, String>) {

    companion object {
        private const val ASSET_ROOT = "offline-map"
        private const val INSTALL_ROOT = "offline-map"
        private const val COMPLETE_MARKER = ".complete"
        private const val GLYPH_EXTENSION = ".pbf"
        val FLAVORS = listOf("light", "dark")

        /**
         * Installs the bundled files if this version is not installed yet, removes older versions, and returns them.
         * Blocking: call off the main thread.
         */
        @Throws(IOException::class)
        fun install(context: Context): OfflineMapAssets {
            val assets = context.assets
            val version = assets.open("$ASSET_ROOT/VERSION").bufferedReader().use { it.readText().trim() }
            val root = File(context.filesDir, INSTALL_ROOT)
            val dir = File(root, version.filter { it.isLetterOrDigit() || it == '-' || it == '.' })

            if (!File(dir, COMPLETE_MARKER).exists()) {
                dir.deleteRecursively()
                copyAssetTree(context, "$ASSET_ROOT/sprites", File(dir, "sprites"))
                installFonts(context, File(dir, "fonts"))
                File(dir, COMPLETE_MARKER).writeText(version)
            }
            root.listFiles()?.filter { it != dir }?.forEach { it.deleteRecursively() }

            val templates =
                FLAVORS.associateWith { flavor ->
                    assets.open("$ASSET_ROOT/styles/$flavor.json").bufferedReader().use { it.readText() }
                }
            return OfflineMapAssets(dir, templates)
        }

        /**
         * Copies each bundled font and fills every glyph range it does not ship with an empty one (see
         * [emptyGlyphRange]). Each font goes in twice, under its name and URL-encoded (`Noto%20Sans%20Regular`): the
         * glyph URL MapLibre builds carries the encoded name, and whether its file source decodes it is not documented,
         * so both spellings resolve.
         */
        private fun installFonts(context: Context, fontsDir: File) {
            val fonts = context.assets.list("$ASSET_ROOT/fonts").orEmpty()
            for (font in fonts) {
                val spellings = setOf(font, URLEncoder.encode(font, "UTF-8").replace("+", "%20"))
                for (spelling in spellings) {
                    val target = File(fontsDir, spelling)
                    copyAssetTree(context, "$ASSET_ROOT/fonts/$font", target)
                    for (range in glyphRanges) {
                        val file = File(target, range + GLYPH_EXTENSION)
                        if (!file.exists()) file.writeBytes(emptyGlyphRange(font, range))
                    }
                }
            }
        }

        private fun copyAssetTree(context: Context, assetPath: String, target: File) {
            target.mkdirs()
            for (name in context.assets.list(assetPath).orEmpty()) {
                context.assets.open("$assetPath/$name").use { input ->
                    File(target, name).outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
