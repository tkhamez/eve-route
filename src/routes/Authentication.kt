package net.tkhamez.everoute.routes

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
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.data.EsiVerify
import net.tkhamez.everoute.data.ResponseAuthUser
import net.tkhamez.everoute.data.Session
import net.tkhamez.everoute.httpClient
import java.lang.Exception

fun Route.authentication() {
    authenticate("eve-oauth") {
        route("/auth/login") {
            handle {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                if (principal != null) {
                    var esiVerify: EsiVerify? = null
                    try {
                        esiVerify = httpClient.get("https://login.eveonline.com/oauth/verify") {
                            header("Authorization", "Bearer ${principal.accessToken}")
                        }
                    } catch(e: Exception) {
                        call.application.environment.log.error(e.message)
                    }
                    if (esiVerify != null) {
                        val session = call.sessions.get<Session>() ?: Session()
                        call.sessions.set(session.copy(
                            esiToken = EsiToken.Data(
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

    get("/auth/user") {
        val session = call.sessions.get<Session>()
        var data: ResponseAuthUser? = null
        if (session?.esiVerify?.CharacterID != null) {
            data = ResponseAuthUser(
                session.esiVerify.CharacterID,
                session.esiVerify.CharacterName
            )
        }
        call.respondText(Gson().toJson(data), contentType = ContentType.Application.Json)
    }

    get("/auth/logout") {
        val session = call.sessions.get<Session>() ?: Session()
        call.sessions.set(session.copy(esiToken = null, esiVerify = null))
        call.respondRedirect("/")
    }
}
