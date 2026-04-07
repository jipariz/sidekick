package dev.parez.sidekick.demo

import dev.parez.sidekick.network.ktor.NetworkMonitorKtor
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val BASE = "https://jsonplaceholder.typicode.com"

val demoHttpClient = HttpClient {
    install(NetworkMonitorKtor) {
        sanitizeHeader { it.equals("Authorization", ignoreCase = true) }
    }
}

suspend fun fetchPosts(): HttpResponse =
    demoHttpClient.get("$BASE/posts")

suspend fun fetchPost(): HttpResponse =
    demoHttpClient.get("$BASE/posts/1")

suspend fun fetchUser(): HttpResponse =
    demoHttpClient.get("$BASE/users/1")

suspend fun createPost(): HttpResponse =
    demoHttpClient.post("$BASE/posts") {
        contentType(ContentType.Application.Json)
        setBody("""{"title":"Sidekick Test","body":"Testing network monitor","userId":1}""")
    }

suspend fun updatePost(): HttpResponse =
    demoHttpClient.put("$BASE/posts/1") {
        contentType(ContentType.Application.Json)
        setBody("""{"id":1,"title":"Updated Post","body":"Updated body","userId":1}""")
    }

suspend fun deletePost(): HttpResponse =
    demoHttpClient.delete("$BASE/posts/1")

suspend fun fetchNotFound(): HttpResponse =
    demoHttpClient.get("$BASE/posts/99999")
