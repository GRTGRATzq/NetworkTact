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
// Essai de l'extracteur Kotlin de NetworkTact contre une vraie édition Protomaps, hors de l'application (tâche 7,
// docs/PLAN-CARTE-TELECHARGEMENT.md). Compilé par le workflow fork-carte-interop avec les sources de
// feature/map/.../offline/extract : c'est le même code que celui du téléphone, avec un moteur HTTP de la JVM.
//
// Usage : ExtractCli --bbox OUEST,SUD,EST,NORD --max-zoom 15 --out carte.pmtiles [--base https://build.protomaps.com]
// Écrit aussi edition.txt (adresse de l'édition utilisée) et extract-stats.json.
package org.meshtastic.feature.map.offline.extract

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

private class CountingSource(private val inner: RangeSource) : RangeSource {
    var requests = 0
    var bytes = 0L

    override suspend fun read(offset: Long, length: Long): ByteArray =
        inner.read(offset, length).also {
            requests++
            bytes += it.size
        }
}

fun main(args: Array<String>) {
    val options = args.toList().chunked(2).associate { (key, value) -> key.removePrefix("--") to value }
    val (west, south, east, north) = options.getValue("bbox").split(",").map { it.toDouble() }
    val bounds = GeoBounds(west, south, east, north)
    val maxZoom = options["max-zoom"]?.toInt() ?: 15
    val out = options.getValue("out").toPath()
    val base = options["base"] ?: "https://build.protomaps.com"
    val client =
        HttpClient(Java) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    val code =
        runBlocking {
            client.use { http ->
                val today = LocalDate.now(ZoneOffset.UTC)
                val url =
                    (0L..7L)
                        .map { "$base/${today.minusDays(it).format(DateTimeFormatter.BASIC_ISO_DATE)}.pmtiles" }
                        .firstOrNull { HttpRangeSource(http, it).head().status == 200 }
                if (url == null) {
                    println("Aucune édition sur les 8 derniers jours")
                    return@runBlocking 2
                }
                println("Édition : $url")
                FileSystem.SYSTEM.write("edition.txt".toPath()) { writeUtf8(url) }
                val http1 = HttpRangeSource(http, url)
                val source = CountingSource(http1)
                val extractor = MapExtractor(source)
                val t0 = System.nanoTime()
                val archive = extractor.readSource()
                println("Métadonnées : ${archive.metadataText().take(160)}…")
                val plan = extractor.plan(archive, bounds, 0, maxZoom)
                val planSeconds = (System.nanoTime() - t0) / 1e9
                val planRequests = source.requests
                val planBytes = source.bytes
                println(
                    "Plan : ${plan.tiles.addressedTiles} tuiles, ${plan.tiles.tileEntries} entrées, " +
                        "${plan.tiles.tileContents} contenus, fichier ${plan.fileSize} octets, " +
                        "${plan.ranges.size} requêtes de tuiles (${plan.transferBytes} octets) ; " +
                        "lecture : $planRequests requêtes, $planBytes octets, ${"%.1f".format(planSeconds)} s",
                )
                val part = (out.toString() + ".part").toPath()
                val journalPath = (out.toString() + ".journal.json").toPath()
                val journal =
                    DownloadJournal(
                        sourceUrl = url,
                        etag = http1.etag ?: "",
                        west = west,
                        south = south,
                        east = east,
                        north = north,
                        minZoom = plan.minZoom,
                        maxZoom = plan.maxZoom,
                        totalSize = plan.fileSize,
                        rangeCount = plan.ranges.size,
                        startedAtEpochSeconds = System.currentTimeMillis() / 1000,
                    )
                val t1 = System.nanoTime()
                extractor.download(plan, FileSystem.SYSTEM, part, journalPath, journal)
                val downloadSeconds = (System.nanoTime() - t1) / 1e9
                FileSystem.SYSTEM.atomicMove(part, out)
                FileSystem.SYSTEM.delete(journalPath)
                println(
                    "Téléchargement : ${source.requests - planRequests} requêtes, ${source.bytes - planBytes} octets, " +
                        "${"%.1f".format(downloadSeconds)} s ; contrôle final réussi ; $out écrit",
                )
                FileSystem.SYSTEM.write("extract-stats.json".toPath()) {
                    writeUtf8(
                        """{"edition":"$url","tiles":${plan.tiles.addressedTiles},"entries":${plan.tiles.tileEntries},""" +
                            """"contents":${plan.tiles.tileContents},"fileSize":${plan.fileSize},""" +
                            """"tileRequests":${plan.ranges.size},"transferBytes":${plan.transferBytes},""" +
                            """"planRequests":$planRequests,"planBytes":$planBytes,"planSeconds":$planSeconds,""" +
                            """"downloadSeconds":$downloadSeconds}""",
                    )
                }
                0
            }
        }
    exitProcess(code)
}
