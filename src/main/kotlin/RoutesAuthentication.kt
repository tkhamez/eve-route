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

fun Route.authentication() {
    authenticate("eve-oauth") {
        route("/login") {
            handle {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                if (principal != null) {
                    val eveAuth = httpClient.get<EsiEveAuth>("https://login.eveonline.com/oauth/verify") {
                        header("Authorization", "Bearer ${principal.accessToken}")
                    }
                    if (eveAuth.CharacterID != null) {
                        val session = call.sessions.get<Session>() ?: Session()
                        call.sessions.set(session.copy(eveAuth = mutableMapOf(
                                "id" to eveAuth.CharacterID,
                                "name" to eveAuth.CharacterName,
                                "accessToken" to principal.accessToken,
                                "scopes" to eveAuth.Scopes,
                                "expiresOn" to eveAuth.ExpiresOn,
                                "refreshToken" to principal.refreshToken
                        )))
                    }
                }
                call.respondRedirect("/")
            }
        }
    }

    get("/api/user") {
        val session = call.sessions.get<Session>()
        var data = emptyMap<String, Any?>()
        if (session?.eveAuth != null && session.eveAuth.containsKey("id")) {
            data = mapOf(
                    "id" to session.eveAuth["id"],
                    "name" to session.eveAuth["name"]
            )
        }
        call.respondText(Gson().toJson(data), contentType = ContentType.Application.Json)
    }

    get("/logout") {
        val session = call.sessions.get<Session>() ?: Session()
        call.sessions.set(session.copy(eveAuth = mutableMapOf()))
        call.respondRedirect("/")
    }
}
