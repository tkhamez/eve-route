package net.tkhamez.everoute

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import java.lang.Exception

fun Route.authentication() {
    authenticate("eve-oauth") {
        route("/login") {
            handle {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                if (principal != null) {
                    var esiVerify: EsiVerify? = null
                    try {
                        esiVerify = httpClient.get("https://login.eveonline.com/oauth/verify") {
                            header("Authorization", "Bearer ${principal.accessToken}")
                        }
                    } catch(e: Exception) {
                        println(e.message)
                    }
                    if (esiVerify != null) {
                        val session = call.sessions.get<Session>() ?: Session()
                        call.sessions.set(session.copy(
                            authToken = AuthToken(
                                accessToken = principal.accessToken,
                                refreshToken = principal.refreshToken.toString(),
                                expiresOn = esiVerify.ExpiresOn
                            ),
                            esiVerify = esiVerify
                        ))
                    }
                }
                call.respondRedirect("/")
            }
        }
    }

    get("/api/user") {
        val session = call.sessions.get<Session>()
        var data: ResponseUser? = null
        if (session?.esiVerify?.CharacterID != null) {
            data = ResponseUser(session.esiVerify.CharacterID, session.esiVerify.CharacterName)
        }
        call.respondText(Gson().toJson(data), contentType = ContentType.Application.Json)
    }

    get("/logout") {
        val session = call.sessions.get<Session>() ?: Session()
        call.sessions.set(session.copy(authToken = null, esiVerify = null))
        call.respondRedirect("/")
    }
}
