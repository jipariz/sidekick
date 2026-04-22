package dev.parez.sidekick.network.ktor

import dev.parez.sidekick.network.NetworkMonitorStore
import dev.parez.sidekick.network.currentTimeMillis
import io.ktor.client.call.save
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
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

        on(Send) { request ->
            // ── Filter ────────────────────────────────────────────────────────
            if (!config.shouldLog(request)) {
                request.attributes.put(DisableLogging, Unit)
                return@on proceed(request)
            }

            val id = Uuid.random().toString()
            request.attributes.put(CallId, id)

            // ── Request capture ───────────────────────────────────────────────
            val reqHeaders = request.headers.build().sanitize(config.sanitizedHeaders)
            val reqBody = runCatching { request.body.toString() }.getOrNull()
                ?.takeIf { it != "EmptyContent" }
                ?.truncate(config.maxContentLength)

            val url = request.url.buildString()
            val method = request.method.value

            runCatching {
                store.recordRequest(
                    id = id,
                    url = url,
                    method = method,
                    headers = reqHeaders,
                    body = reqBody,
                    timestamp = currentTimeMillis(),
                )
            }

            // ── Execute request ───────────────────────────────────────────────
            val call = try {
                proceed(request)
            } catch (e: Throwable) {
                runCatching { store.recordError(id, e) }
                throw e
            }

            // ── Response metadata ─────────────────────────────────────────────
            val statusCode = call.response.status.value
            runCatching {
                store.recordResponse(
                    id = id,
                    code = statusCode,
                    headers = call.response.headers.sanitize(config.sanitizedHeaders),
                    timestamp = currentTimeMillis(),
                )
            }

            // ── Response body ─────────────────────────────────────────────────
            // Buffer the response in memory via call.save() so we can capture
            // the body text AND still return intact bytes for downstream use
            // (e.g. ContentNegotiation deserialisation). Binary responses are
            // passed through unchanged to avoid unnecessary memory pressure.
            // truncate() caps what is stored; isTextBased() already prevents
            // buffering binary payloads (images, audio, protobuf, etc.).
            val contentType = call.response.contentType()
            if (contentType == null || contentType.isTextBased()) {
                val savedCall = call.save()
                runCatching {
                    val charset = contentType?.charset() ?: io.ktor.utils.io.charsets.Charsets.UTF_8
                    val body = savedCall.response.bodyAsText(charset)
                    store.recordResponseBody(id = id, body = body.truncate(config.maxContentLength))
                }
                savedCall
            } else {
                call
            }
        }
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
