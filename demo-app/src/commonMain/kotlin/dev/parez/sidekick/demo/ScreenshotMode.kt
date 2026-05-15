package dev.parez.sidekick.demo

import dev.parez.sidekick.logs.LogLevel
import dev.parez.sidekick.logs.LogMonitorStore
import dev.parez.sidekick.network.di.NetworkMonitorKoinContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configures the demo app for reproducible screenshot capture.
 *
 * Activated by the `SIDEKICK_SHOT` env var (read in `jvmMain/main.kt`):
 *
 *  - `hero`        — Pokémon catalog only, no Sidekick panel.
 *  - `menu`        — Sidekick panel open at the plugin grid.
 *  - `network`     — Open Sidekick directly on the Network Monitor with seeded calls.
 *  - `logs`        — Open Sidekick directly on the Log Monitor with seeded entries.
 *  - `preferences` — Open Sidekick directly on the Preferences screen.
 *  - `custom`      — Open Sidekick directly on the Custom Debug screen.
 */
data class ScreenshotConfig(val target: String?) {
    val isActive: Boolean get() = target != null
    val openSidekickOnLaunch: Boolean get() = target != null && target != "hero"
    val initialPluginId: String? get() = when (target) {
        "network"     -> "network-monitor"
        "logs"        -> "log-monitor"
        "preferences" -> "preferences"
        "custom"      -> "build-info"
        else -> null
    }
}

internal fun seedScreenshotData(scope: CoroutineScope, currentTimeMillis: () -> Long) {
    scope.launch {
        delay(300)
        seedNetworkCalls(currentTimeMillis())
        seedLogs()
    }
}

private suspend fun seedNetworkCalls(now: Long) {
    val store = NetworkMonitorKoinContext.getDefaultStore()
    val samples = listOf(
        NetSample("GET",    "https://pokeapi.co/api/v2/pokemon?limit=20&offset=0", 200, 142,  pokemonListJson),
        NetSample("GET",    "https://pokeapi.co/api/v2/pokemon/pikachu",           200, 318,  pokemonDetailJson),
        NetSample("GET",    "https://pokeapi.co/api/v2/pokemon/charizard",         200, 297,  pokemonDetailJsonAlt),
        NetSample("GET",    "https://pokeapi.co/api/v2/type/electric",             200,  88,  """{"name":"electric","damage_relations":{"double_damage_to":[{"name":"flying"}]}}"""),
        NetSample("POST",   "https://api.example.com/v1/favorites",                201,  64,  """{"id":"fav_19a","pokemon":"pikachu","createdAt":"2026-05-15T10:32:00Z"}"""),
        NetSample("PUT",    "https://api.example.com/v1/profile",                  204,  41,  ""),
        NetSample("GET",    "https://pokeapi.co/api/v2/pokemon/mewtwo",            304,  12,  ""),
        NetSample("DELETE", "https://api.example.com/v1/favorites/fav_8c4",        404,  73,  """{"error":"not_found","message":"Favorite fav_8c4 does not exist"}"""),
        NetSample("PATCH",  "https://api.example.com/v1/preferences",              400,  55,  """{"error":"validation_failed","field":"theme","reason":"unknown enum value"}"""),
        NetSample("GET",    "https://pokeapi.co/api/v2/ability/lightning-rod",     500, 210,  """{"error":"internal_server_error","trace":"5e2d-9b4c-7711"}"""),
    )
    samples.forEachIndexed { index, sample ->
        val id = "shot-call-${index.toString().padStart(3, '0')}"
        val requestTs = now - (samples.size - index) * 3_500L
        store.recordRequest(
            id = id,
            url = sample.url,
            method = sample.method,
            headers = mapOf(
                "Accept" to "application/json",
                "User-Agent" to "Sidekick-Demo/0.2.0",
                "Authorization" to "Bearer eyJhbGciOi...<redacted>",
            ),
            body = null,
            timestamp = requestTs,
        )
        store.recordResponse(
            id = id,
            code = sample.status,
            headers = mapOf(
                "Content-Type" to "application/json; charset=utf-8",
                "Cache-Control" to "public, max-age=300",
            ),
            timestamp = requestTs + sample.durationMs,
        )
        if (sample.body.isNotEmpty()) {
            store.recordResponseBody(id = id, body = sample.body)
        }
    }
}

private data class NetSample(
    val method: String,
    val url: String,
    val status: Int,
    val durationMs: Long,
    val body: String,
)

private val pokemonListJson = """
    {
      "count": 1302,
      "next": "https://pokeapi.co/api/v2/pokemon?offset=20&limit=20",
      "results": [
        {"name":"bulbasaur","url":"https://pokeapi.co/api/v2/pokemon/1/"},
        {"name":"ivysaur","url":"https://pokeapi.co/api/v2/pokemon/2/"},
        {"name":"venusaur","url":"https://pokeapi.co/api/v2/pokemon/3/"}
      ]
    }
""".trimIndent()

private val pokemonDetailJson = """
    {
      "id": 25,
      "name": "pikachu",
      "height": 4,
      "weight": 60,
      "base_experience": 112,
      "types": [{"slot":1,"type":{"name":"electric"}}]
    }
""".trimIndent()

private val pokemonDetailJsonAlt = """
    {
      "id": 6,
      "name": "charizard",
      "height": 17,
      "weight": 905,
      "base_experience": 267,
      "types": [{"slot":1,"type":{"name":"fire"}},{"slot":2,"type":{"name":"flying"}}]
    }
""".trimIndent()

private suspend fun seedLogs() {
    delay(50)
    val store = LogMonitorStore
    val logs = listOf(
        LogSample(LogLevel.INFO,    "AppLifecycle",  "Application started - environment=demo, build=debug"),
        LogSample(LogLevel.DEBUG,   "Network",       "GET /pokemon?limit=20&offset=0 -> 200 (142ms)"),
        LogSample(LogLevel.DEBUG,   "Network",       "GET /pokemon/pikachu -> 200 (318ms)"),
        LogSample(LogLevel.INFO,    "PokemonRepo",   "Cached 20 Pokemon entries in Room (TTL 24h)"),
        LogSample(LogLevel.VERBOSE, "Compose",       "Recomposed PokemonListScreen (scope=feed)"),
        LogSample(LogLevel.DEBUG,   "Auth",          "Session token refresh scheduled in 4h 12m"),
        LogSample(LogLevel.WARN,    "Network",       "Slow response: GET /ability/lightning-rod took 1.2s (p99=400ms)"),
        LogSample(LogLevel.INFO,    "PrefsStore",    "Preference changed: dark_mode true -> false"),
        LogSample(LogLevel.ERROR,   "Network",       "GET /ability/lightning-rod -> 500 internal_server_error (trace 5e2d-9b4c-7711)"),
        LogSample(LogLevel.WARN,    "PokemonRepo",   "Cache miss for pokemon=missingno - falling back to network"),
        LogSample(LogLevel.DEBUG,   "Analytics",     "Event tracked: pokemon_viewed { id=25, source=catalog }"),
        LogSample(LogLevel.INFO,    "AppLifecycle",  "Pokedex ready - 20 entries loaded in 421ms"),
    )
    logs.forEach { sample ->
        store.record(
            level = sample.level,
            tag = sample.tag,
            message = sample.message,
            throwable = null,
        )
        delay(15)
    }
}

private data class LogSample(val level: LogLevel, val tag: String, val message: String)
