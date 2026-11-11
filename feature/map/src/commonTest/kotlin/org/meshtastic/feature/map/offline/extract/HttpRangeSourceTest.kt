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
package org.meshtastic.feature.map.offline.extract

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/** What an engine throws on a dead network (UnknownHostException, SocketException…): any exception of its own. */
private class FakeNetworkError(message: String) : Exception(message)

class HttpRangeSourceTest {

    private val file = ByteArray(10_000) { (it % 251).toByte() }
    private val url = "https://maps.example/20261111.pmtiles"
    private val requests = ArrayList<HttpRequestData>()

    /** A server holding [file], like the Protomaps one (probe of 11 November 2026). */
    private fun MockRequestHandleScope.serve(request: HttpRequestData, etag: String = "\"e1\""): HttpResponseData {
        val ifMatch = request.headers[HttpHeaders.IfMatch]
        if (ifMatch != null && ifMatch != etag) return respond("", HttpStatusCode.PreconditionFailed)
        val range = request.headers[HttpHeaders.Range]!!.removePrefix("bytes=").split("-").map { it.toLong() }
        val body = file.copyOfRange(range[0].toInt(), range[1].toInt() + 1)
        return respond(
            ByteReadChannel(body),
            HttpStatusCode.PartialContent,
            headersOf(
                HttpHeaders.ContentRange to listOf("bytes ${range[0]}-${range[1]}/${file.size}"),
                HttpHeaders.ETag to listOf(etag),
            ),
        )
    }

    private fun source(
        etag: String? = null,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpRangeSource(
        HttpClient(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        ),
        url,
        etag,
        RetryPolicy(listOf(1_000L, 3_000L, 9_000L)),
    )

    @Test
    fun readsExactlyOneRangeWithAFixedUserAgent() = runTest {
        val source = source { serve(it) }
        assertContentEquals(file.copyOfRange(16, 116), source.read(16, 100))
        val request = requests.single()
        assertEquals("bytes=16-115", request.headers[HttpHeaders.Range])
        assertEquals("pmtiles-client/1.0", request.headers[HttpHeaders.UserAgent])
        assertNull(request.headers[HttpHeaders.IfMatch], "no ETag known yet")
        assertEquals("\"e1\"", source.etag)
    }

    @Test
    fun laterRequestsCarryTheEtagOfTheFirstAnswer() = runTest {
        val source = source { serve(it) }
        source.read(0, 10)
        source.read(500, 10)
        assertEquals("\"e1\"", requests.last().headers[HttpHeaders.IfMatch])
    }

    @Test
    fun aReplacedFileStopsTheDownload() = runTest {
        assertFailsWith<MapDownloadException.EditionChanged> { source(etag = "\"old\"") { serve(it) }.read(0, 10) }
        requests.clear()
        // A server that ignores If-Match but sends another ETag.
        val source = source { serve(it, etag = if (requests.size == 1) "\"e1\"" else "\"e2\"") }
        source.read(0, 10)
        assertFailsWith<MapDownloadException.EditionChanged> { source.read(10, 10) }
        assertEquals(2, requests.size, "never retried")
    }

    @Test
    fun theWholeFileIsRefusedWithoutRetry() = runTest {
        val error =
            assertFailsWith<MapDownloadException.RangesUnsupported> {
                source { respond(ByteReadChannel(file), HttpStatusCode.OK) }.read(0, 10)
            }
        assertEquals(true, error.message!!.contains("whole file"))
        assertEquals(1, requests.size)
    }

    @Test
    fun aMismatchedContentRangeIsRefused() = runTest {
        assertFailsWith<MapDownloadException.RangesUnsupported> {
            source {
                respond(
                    ByteReadChannel(ByteArray(10)),
                    HttpStatusCode.PartialContent,
                    headersOf(HttpHeaders.ContentRange, "bytes 0-9/${file.size}"),
                )
            }
                .read(100, 10)
        }
    }

    @Test
    fun missingEditionIsNotFoundAtOnce() = runTest {
        val error =
            assertFailsWith<MapDownloadException.NotFound> {
                source { respond("", HttpStatusCode.NotFound) }.read(0, 10)
            }
        assertEquals(404, error.status)
        assertEquals(1, requests.size)
    }

    @Test
    fun busyServerIsRetriedAfterOneThreeAndNineSeconds() = runTest {
        val source = source { if (requests.size <= 2) respond("", HttpStatusCode.ServiceUnavailable) else serve(it) }
        val started = currentTime
        assertContentEquals(file.copyOfRange(0, 10), source.read(0, 10))
        assertEquals(3, requests.size)
        assertEquals(4_000L, currentTime - started)
    }

    @Test
    fun aServerStillBusyAfterTheRetriesIsReported() = runTest {
        val error =
            assertFailsWith<MapDownloadException.ServerBusy> {
                source { respond("", HttpStatusCode.TooManyRequests) }.read(0, 10)
            }
        assertEquals(429, error.status)
        assertEquals(4, requests.size)
    }

    @Test
    fun networkErrorsAreRetriedThenReportedAsUnreachable() = runTest {
        val error =
            assertFailsWith<MapDownloadException.Unreachable> {
                source { throw FakeNetworkError("connection reset") }.read(0, 10)
            }
        assertIs<FakeNetworkError>(error.cause)
        assertEquals(4, requests.size)
    }

    @Test
    fun aShortBodyIsRetried() = runTest {
        val source = source {
            if (requests.size == 1) {
                respond(
                    ByteReadChannel(ByteArray(5)),
                    HttpStatusCode.PartialContent,
                    headersOf(HttpHeaders.ContentRange, "bytes 0-9/${file.size}"),
                )
            } else {
                serve(it)
            }
        }
        assertContentEquals(file.copyOfRange(0, 10), source.read(0, 10))
        assertEquals(2, requests.size)
    }

    @Test
    fun cancellationIsNotTurnedIntoAnError() = runTest {
        val source = source { throw CancellationException("stopped") }
        assertFailsWith<CancellationException> { source.read(0, 10) }
        assertEquals(1, requests.size)
    }

    @Test
    fun headReportsWhatTheServerSays() = runTest {
        val source = source {
            assertEquals(HttpMethod.Head, it.method)
            respond(
                "",
                HttpStatusCode.OK,
                headersOf(
                    HttpHeaders.ETag to listOf("\"e1\""),
                    HttpHeaders.ContentLength to listOf("139406074356"),
                    HttpHeaders.AcceptRanges to listOf("bytes"),
                    HttpHeaders.Date to listOf("Wed, 11 Nov 2026 15:23:29 GMT"),
                ),
            )
        }
        val info = source.head()
        assertEquals(RemoteFileInfo(200, "\"e1\"", 139_406_074_356L, true, "Wed, 11 Nov 2026 15:23:29 GMT"), info)
        assertEquals(1, requests.size)
    }

    @Test
    fun headDoesNotRetry() = runTest {
        assertFailsWith<MapDownloadException.Unreachable> { source { throw FakeNetworkError("no route") }.head() }
        assertEquals(1, requests.size)
    }

    @Test
    fun parallelReadsShareTheSource() = runTest {
        val source = source { serve(it) }
        val parts = List(4) { i -> async { source.read(i * 1_000L, 1_000) } }.map { it.await() }
        assertContentEquals(file.copyOfRange(0, 4_000), parts.reduce { a, b -> a + b })
    }
}
