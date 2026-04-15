package dev.parez.sidekick.demo

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
        }
    }
}
