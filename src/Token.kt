package net.tkhamez.everoute

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class Token(private val config: Config) {

    suspend fun getAccessToken(authToken: AuthToken): AuthToken {
        val dateFormat = "yyyy-MM-dd'T'HH:mm:ss"

        val df: DateFormat = SimpleDateFormat(dateFormat)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val expiresOn = df.parse(authToken.expiresOn)
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        var token: EsiRefreshToken? = null
        if (expiresOn.time - now.timeInMillis <= 60 * 1000) {
            token = refreshToken(authToken.refreshToken)
        }

        if (token != null) {
            val newExpiresOn = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            newExpiresOn.add(Calendar.SECOND, token.expires_in)
            authToken.expiresOn = SimpleDateFormat(dateFormat).format(newExpiresOn.time)
            authToken.accessToken = token.access_token
        }

        return authToken
    }

    private suspend fun refreshToken(refreshToken: String): EsiRefreshToken? {
        val auth = Base64.getEncoder().encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())
        val response: EsiRefreshToken
        try {
            response = httpClient.post(config.accessTokenUrl) {
                header("Authorization", "Basic $auth")
                body = TextContent(
                        "grant_type=refresh_token&refresh_token=$refreshToken",
                        ContentType.Application.FormUrlEncoded
                )
            }
        } catch (e: Exception) {
            println(e.message)
            return null
        }

        return response
    }
}
