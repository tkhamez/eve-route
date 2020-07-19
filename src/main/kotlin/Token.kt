package net.tkhamez.everoute

import io.ktor.client.features.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class Token(
        private val accessTokenUrl: String,
        private val clientId: String,
        private val clientSecret: String
) {
    private val dateFormat = "yyyy-MM-dd'T'HH:mm:ss"

    suspend fun getAccessToken(eveAuth: Map<String, Any?>): EsiRefreshToken {
        val df: DateFormat = SimpleDateFormat(dateFormat)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val expiresOn = df.parse(eveAuth["expiresOn"].toString())
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        var token: EsiRefreshToken? = null
        if (expiresOn.time - now.timeInMillis <= 60 * 1000) {
            token = refreshToken(eveAuth["refreshToken"].toString())
        }

        return token
                ?: EsiRefreshToken(
                        access_token = eveAuth["accessToken"].toString(),
                        expires_in = 0, // it's not used
                        expiresOn = eveAuth["expiresOn"].toString()
                )
    }

    private suspend fun refreshToken(refreshToken: String): EsiRefreshToken? {
        val auth: String = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val response: EsiRefreshToken
        try {
            response = httpClient.post(accessTokenUrl) {
                header("Authorization", "Basic $auth")
                body = TextContent(
                        "grant_type=refresh_token&refresh_token=$refreshToken",
                        ContentType.Application.FormUrlEncoded
                )
            }
        } catch (e: ResponseException) {
            println(e.message)
            return null
        }

        val expiresOn = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        expiresOn.add(Calendar.SECOND, response.expires_in)
        response.expiresOn = SimpleDateFormat(dateFormat).format(expiresOn.time)

        return response
    }
}
