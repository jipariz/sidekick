package dev.parez.sidekick.demo

import dev.parez.sidekick.logs.LogLevel
import dev.parez.sidekick.logs.LogMonitorStore
import dev.parez.sidekick.network.RetentionPeriod
import dev.parez.sidekick.network.ktor.NetworkMonitorKtor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val pokeHttpClient: HttpClient by lazy {
    HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(NetworkMonitorKtor) {
            retentionPeriod = RetentionPeriod.ONE_HOUR
            sanitizeHeader { it.equals("Authorization", ignoreCase = true) }
            onRequest = { id, method, url ->
                LogMonitorStore.record(
                    level = LogLevel.INFO,
                    tag = "HTTP",
                    message = "$method $url",
                    throwable = null,
                    metadata = mapOf("networkCallId" to id),
                )
            }
            onResponse = { id, statusCode, url ->
                val level = if (statusCode in 200..399) LogLevel.DEBUG else LogLevel.WARN
                LogMonitorStore.record(
                    level = level,
                    tag = "HTTP",
                    message = "$statusCode $url",
                    throwable = null,
                    metadata = mapOf("networkCallId" to id),
                )
            }
        }
    }
}
