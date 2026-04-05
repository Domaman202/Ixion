package com.kingmang.ixion.modules

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Suppress("unused")
object HttpModule {
    private const val DEFAULT_TIMEOUT_SECONDS: Long = 30
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    @JvmStatic
    fun get(url: String): String {
        return request("GET", url, "")
    }

    @JvmStatic
    fun delete(url: String): String {
        return request("DELETE", url, "")
    }

    @JvmStatic
    fun post(url: String, body: String): String {
        return request("POST", url, body)
    }

    @JvmStatic
    fun put(url: String, body: String): String {
        return request("PUT", url, body)
    }

    @JvmStatic
    fun patch(url: String, body: String): String {
        return request("PATCH", url, body)
    }

    @JvmStatic
    fun request(method: String, url: String, body: String): String {
        val response = send(method, url, body)
        return response.body()
    }

    @JvmStatic
    fun status(method: String, url: String, body: String): Int {
        val response = send(method, url, body)
        return response.statusCode()
    }

    @JvmStatic
    fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    private fun send(methodRaw: String, url: String, body: String): HttpResponse<String> {
        val method = methodRaw.uppercase()
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))

        val request = when (method) {
            "GET" -> requestBuilder.GET().build()
            "DELETE" -> requestBuilder.DELETE().build()
            else -> requestBuilder
                .header("Content-Type", "text/plain; charset=UTF-8")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build()
        }

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }
}
