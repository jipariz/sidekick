package dev.parez.sidekick.demo

import dev.parez.sidekick.network.ktor.NetworkMonitorKtor
import kotlin.time.Duration.Companion.hours
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
            retentionPeriod = 1.hours
            sanitizeHeader { it.equals("Authorization", ignoreCase = true) }
        }
    }
}
