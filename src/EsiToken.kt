package net.tkhamez.everoute

import io.ktor.application.ApplicationCall
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import net.tkhamez.everoute.data.Config
import net.tkhamez.everoute.data.EsiRefreshToken
import net.tkhamez.everoute.data.Session
import org.slf4j.Logger
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EsiToken(private val config: Config, private val call: ApplicationCall) {
    val log: Logger = call.application.environment.log

    data class Data(
        val refreshToken: String,
        var accessToken: String,
        var expiresOn: String
    )

    /**
     * Returns a valid ESI access token or null if no refresh token was found in the session.
     */
    suspend fun get(): String? {
        val session = call.sessions.get<Session>()
        if (session?.esiToken == null) {
            return null
        }

        val esiToken = getToken(session.esiToken)
        call.sessions.set(session.copy(esiToken = esiToken))

        return esiToken.accessToken
    }

    private suspend fun getToken(esiToken: Data): Data {
        val dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        val df: DateFormat = SimpleDateFormat(dateFormat)
        df.timeZone = TimeZone.getTimeZone("UTC")

        val expiresOn = df.parse(esiToken.expiresOn)
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        var token: EsiRefreshToken? = null
        if (expiresOn.time - now.timeInMillis <= 60 * 1000) {
            token = refreshToken(esiToken.refreshToken)
        }

        if (token != null) {
            val newExpiresOn = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            newExpiresOn.add(Calendar.SECOND, token.expires_in)
            esiToken.expiresOn = df.format(newExpiresOn.time)
            esiToken.accessToken = token.access_token
        }

        return esiToken
    }

    private suspend fun refreshToken(refreshToken: String): EsiRefreshToken? {
        val httpRequest = HttpRequest(config, call.application.environment.log)
        val auth = Base64.getEncoder().encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())
        return httpRequest.request<EsiRefreshToken>(
            config.accessTokenUrl,
            HttpMethod.Post,
            TextContent(
                "grant_type=refresh_token&refresh_token=$refreshToken",
                ContentType.Application.FormUrlEncoded
            ),
            auth,
            "Basic"
        )
    }
}
