package net.tkhamez.everoute.routes

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import net.tkhamez.everoute.HttpRequest
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.gson

fun Route.authentication(config: Config) {

    /**
     * Intercept all non-public routes below /api/ and return 403 if client is not logged in.
     */
    intercept(ApplicationCallPipeline.Features) {
        val path = call.request.path()
        val publicRoutes = listOf(
            "/api/auth/login",
            "/api/auth/logout" // to make sure that the redirect is done
        )
        if (path.indexOf("/api/") == 0 && publicRoutes.indexOf(path) == -1) {
            val session = call.sessions.get<Session>()
            if (session?.esiVerify == null) {
                call.respond(HttpStatusCode.Forbidden, "")
                return@intercept finish()
            }
        }
    }

    authenticate("eve-oauth") {
        route("/api/auth/login") {
            handle {
                val httpRequest = HttpRequest(config, call.application.environment.log)
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                if (principal != null) {
                    val esiVerify = httpRequest.request<EsiVerify>(
                        config.verifyUrl,
                        HttpMethod.Get,
                        null,
                        principal.accessToken
                    )
                    if (esiVerify != null) {
                        val esiAffiliation = httpRequest.post<Array<EsiAffiliation>>(
                            "latest/characters/affiliation/?datasource=${config.esiDatasource}",
                            "[${esiVerify.CharacterID}]"
                        )
                        val session = call.sessions.get<Session>() ?: Session()
                        call.sessions.set(session.copy(
                            esiToken = EsiToken.Data(
                                accessToken = principal.accessToken,
                                refreshToken = principal.refreshToken.toString(),
                                expiresOn = esiVerify.ExpiresOn
                            ),
                            esiVerify = esiVerify,
                            esiAffiliation = esiAffiliation?.get(0)
                        ))
                    }
                }
                call.respondRedirect("/")
            }
        }
    }

    get("/api/auth/user") {
        val session = call.sessions.get<Session>()
        var data: ResponseAuthUser? = null
        if (session?.esiVerify?.CharacterID != null) {
            data = ResponseAuthUser(
                session.esiVerify.CharacterID,
                session.esiVerify.CharacterName,
                session.esiAffiliation?.alliance_id
            )
        }
        call.respondText(gson.toJson(data), contentType = ContentType.Application.Json)
    }

    get("/api/auth/logout") {
        val session = call.sessions.get<Session>() ?: Session()
        call.sessions.set(session.copy(esiToken = null, esiVerify = null))
        call.respondRedirect("/")
    }
}
