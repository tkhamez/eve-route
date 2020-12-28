package net.tkhamez.everoute

import io.ktor.client.features.*
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.utils.io.*
import net.tkhamez.everoute.data.Config
import org.slf4j.Logger

/**
 * Makes HTTP requests
 *
 * Domain defaults to ESI domain, it adds "Authorization: Bearer" header
 * if an access token is provided.
 */
class HttpRequest(val config: Config, val log: Logger) {
    suspend inline fun <reified T> get(path: String, accessToken: String? = null): T? {
        return request("${config.esiDomain}/$path", HttpMethod.Get, null, accessToken)
    }

    suspend inline fun <reified T> post(path: String, requestBody: String? = null, accessToken: String? = null): T? {
        return request("${config.esiDomain}/$path", HttpMethod.Post, requestBody, accessToken)
    }

    suspend inline fun <reified T> request(
        url: String,
        httpMethod: HttpMethod = HttpMethod.Get,
        requestBody: Any? = null,
        authToken: String? = null,
        authType: String? = "Bearer"
    ): T? {
        var result: T? = null
        try {
            result = httpClient.request<T>(url) {
                method = httpMethod
                if (authToken != null) header("Authorization", "$authType $authToken")
                if (requestBody != null) body = requestBody
            }
        } catch (e: ResponseException) {
            log.error(e.message +"\n"+ e.response.content.readRemaining().readText())
        }
        return result
    }
}
