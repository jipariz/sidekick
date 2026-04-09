package dev.parez.sidekick.network.ktor

import dev.parez.sidekick.network.NetworkMonitorStore
import dev.parez.sidekick.network.currentTimeMillis
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.charset
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import io.ktor.util.toMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val CallId = AttributeKey<String>("NetworkMonitorCallId")
private val DisableLogging = AttributeKey<Unit>("NetworkMonitorDisableLogging")

@OptIn(ExperimentalUuidApi::class)
public val NetworkMonitorKtor: ClientPlugin<NetworkMonitorKtorConfig> =
    createClientPlugin("NetworkMonitorKtor", ::NetworkMonitorKtorConfig) {

        val config = pluginConfig
        val store = config.store

        // ── Request capture ────────────────────────────────────────────────────
        on(Send) { request ->
            if (!config.shouldLog(request)) {
                request.attributes.put(DisableLogging, Unit)
                return@on proceed(request)
            }

            val id = Uuid.random().toString()
            request.attributes.put(CallId, id)

            val headers = request.headers.build().sanitize(config.sanitizedHeaders)
            val bodyText = runCatching { request.body.toString() }.getOrNull()
                ?.takeIf { it != "EmptyContent" }
                ?.truncate(config.maxContentLength)

            runCatching {
                store.recordRequest(
                    id = id,
                    url = request.url.buildString(),
                    method = request.method.value,
                    headers = headers,
                    body = bodyText,
                    timestamp = currentTimeMillis(),
                )
            }

            try {
                proceed(request)
            } catch (e: Throwable) {
                runCatching { store.recordError(id, e) }
                throw e
            }
        }

        // ── Response capture ───────────────────────────────────────────────────
        ResponseObserver.install(
            ResponseObserver.prepare {
                onResponse { response ->
                    val id = response.call.attributes.getOrNull(CallId) ?: return@onResponse

                    // Record metadata independently so it is always captured even if
                    // body reading fails or is skipped.
                    runCatching {
                        store.recordResponse(
                            id = id,
                            code = response.status.value,
                            headers = response.headers.sanitize(config.sanitizedHeaders),
                            timestamp = currentTimeMillis(),
                        )
                    }

                    // Only read the body as text for text-based content types; binary
                    // responses (images, protobuf, etc.) are silently skipped to avoid
                    // charset mis-decoding and unnecessary memory pressure.
                    val contentType = response.contentType()
                    if (contentType == null || contentType.isTextBased()) {
                        runCatching {
                            val charset = contentType?.charset() ?: io.ktor.utils.io.charsets.Charsets.UTF_8
                            val body = response.bodyAsText(charset)
                            store.recordResponseBody(id = id, body = body.truncate(config.maxContentLength))
                        }
                    }
                }
            },
            client,
        )
    }

private fun Headers.sanitize(sanitized: List<SanitizedHeader>): Map<String, String> =
    toMap().mapValues { (key, values) ->
        val match = sanitized.firstOrNull { it.predicate(key) }
        if (match != null) match.placeholder else values.joinToString(", ")
    }

private fun String.truncate(max: Int) = if (length > max) take(max) + "…" else this

/**
 * Returns true if this content type carries human-readable text that can be safely
 * decoded as a string. Binary types (images, audio, protobuf, etc.) return false.
 */
private fun ContentType.isTextBased(): Boolean =
    contentType == "text" ||
        (contentType == "application" && contentSubtype in TEXT_APPLICATION_SUBTYPES)

private val TEXT_APPLICATION_SUBTYPES = setOf(
    "json", "xml", "x-www-form-urlencoded",
    "graphql", "ld+json", "x-ndjson", "x-yaml", "yaml",
)
