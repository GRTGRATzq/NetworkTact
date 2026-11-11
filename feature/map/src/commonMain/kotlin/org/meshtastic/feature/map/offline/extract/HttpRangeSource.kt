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
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Reads parts of a remote archive. */
fun interface RangeSource {
    /** Exactly [length] bytes from [offset], or a [MapDownloadException]. */
    suspend fun read(offset: Long, length: Long): ByteArray
}

/** What a HEAD request says about a remote file. [date] is the server's `Date` header, as sent. */
data class RemoteFileInfo(
    val status: Int,
    val etag: String?,
    val length: Long?,
    val acceptsRanges: Boolean,
    val date: String?,
)

/** Waits before each retry: one retry per element. Three retries after 1, 3 and 9 s by default (plan §9). */
class RetryPolicy(val delaysMillis: List<Long> = DEFAULT_DELAYS_MILLIS) {
    private companion object {
        val DEFAULT_DELAYS_MILLIS = listOf(1_000L, 3_000L, 9_000L)
    }
}

/**
 * A [RangeSource] over HTTP: one `Range: bytes=a-b` GET per call, never several ranges in one request (the Protomaps
 * server answers 400 to that, probe of 11 November 2026).
 *
 * Every request carries `If-Match` with the ETag of the first answer, so a file replaced on the server stops the
 * download (412) instead of mixing two editions; a 200 (the whole file, 139 GB for a Protomaps build) is refused
 * without reading its body. Network errors, 429 and 5xx are retried as [retry] says; 404, 412 and the other refusals
 * are not. The `User-Agent` is the same fixed word on every phone: no app version, no device model.
 *
 * Timeouts are the client's: install `HttpTimeout` on it.
 */
class HttpRangeSource(
    private val client: HttpClient,
    private val url: String,
    etag: String? = null,
    private val retry: RetryPolicy = RetryPolicy(),
) : RangeSource {

    /** The ETag every answer must carry: given, or taken from the first answer. */
    var etag: String? = etag
        private set

    override suspend fun read(offset: Long, length: Long): ByteArray {
        require(offset >= 0 && length > 0) { "range $offset+$length" }
        return withRetries(retry.delaysMillis) { readOnce(offset, length) }
    }

    /** A HEAD request on [url]: status, ETag, length and the server's date, without retries. */
    suspend fun head(): RemoteFileInfo = withRetries(retries = emptyList()) {
        val response = client.head(url) { header(HttpHeaders.UserAgent, USER_AGENT) }
        RemoteFileInfo(
            status = response.status.value,
            etag = response.headers[HttpHeaders.ETag],
            length = response.headers[HttpHeaders.ContentLength]?.toLongOrNull(),
            acceptsRanges = response.headers[HttpHeaders.AcceptRanges]?.contains("bytes") == true,
            date = response.headers[HttpHeaders.Date],
        )
    }

    private suspend fun readOnce(offset: Long, length: Long): ByteArray {
        val last = offset + length - 1
        return client
            .prepareGet(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Range, "bytes=$offset-$last")
                etag?.let { header(HttpHeaders.IfMatch, it) }
            }
            .execute { response -> body(response, offset, last, length) }
    }

    private suspend fun body(response: HttpResponse, offset: Long, last: Long, length: Long): ByteArray {
        val status = response.status.value
        val contentRange = response.headers[HttpHeaders.ContentRange]
        val answered = response.headers[HttpHeaders.ETag]
        val expected = etag
        val problem =
            when {
                status != HttpStatusCode.PartialContent.value -> refusal(status)

                contentRange?.startsWith("bytes $offset-$last/") != true ->
                    MapDownloadException.RangesUnsupported("asked $offset-$last, got $contentRange")

                expected != null && answered != null && answered != expected -> MapDownloadException.EditionChanged()

                else -> null
            }
        if (problem != null) throw problem
        if (expected == null) etag = answered
        val bytes = response.readRawBytes()
        if (bytes.size.toLong() != length) throw TruncatedBody(bytes.size.toLong(), length)
        return bytes
    }

    private fun refusal(status: Int): Exception = when (status) {
        HttpStatusCode.OK.value -> MapDownloadException.RangesUnsupported("whole file sent")

        HttpStatusCode.RequestedRangeNotSatisfiable.value -> MapDownloadException.RangesUnsupported("416")

        HttpStatusCode.NotFound.value,
        HttpStatusCode.Gone.value,
        -> MapDownloadException.NotFound(status)

        HttpStatusCode.PreconditionFailed.value -> MapDownloadException.EditionChanged()

        HttpStatusCode.TooManyRequests.value,
        in SERVER_ERRORS,
        -> Busy(status)

        else -> MapDownloadException.UnexpectedStatus(status)
    }

    private suspend fun <T> withRetries(retries: List<Long>, block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return guarded(block)
            } catch (e: Retryable) {
                if (attempt >= retries.size) throw e.final()
                delay(retries[attempt++])
            }
        }
    }

    /** Turns any failure that is not ours into a retryable network error; lets cancellation through. */
    @Suppress("TooGenericExceptionCaught") // Engines throw their own types (UnknownHostException, SSLException, …).
    private suspend fun <T> guarded(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: MapDownloadException) {
            throw e
        } catch (e: Retryable) {
            throw e
        } catch (e: Exception) {
            throw NetworkFailure(e)
        }

    /** A failure worth another try; [final] is what the caller sees once the retries are spent. */
    private abstract class Retryable(cause: Throwable? = null) : Exception(cause) {
        abstract fun final(): MapDownloadException
    }

    private class NetworkFailure(cause: Throwable) : Retryable(cause) {
        override fun final() = MapDownloadException.Unreachable(cause)
    }

    private class TruncatedBody(received: Long, expected: Long) :
        Retryable(IllegalStateException("$received of $expected bytes")) {
        override fun final() = MapDownloadException.Unreachable(cause)
    }

    private class Busy(val status: Int) : Retryable() {
        override fun final() = MapDownloadException.ServerBusy(status)
    }

    companion object {
        /** The same on every phone: it says what asks, not who. */
        const val USER_AGENT = "NetworkTact"
        private val SERVER_ERRORS = 500..599
    }
}
