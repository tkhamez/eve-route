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
            "/api/auth/login"
        )
        if (path.indexOf("/api/") == 0 && publicRoutes.indexOf(path) == -1) {
            val session = call.sessions.get<Session>()
            if (session?.eveCharacter == null) {
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

                var esiVerify: EsiVerify? = null
                if (principal != null) {
                    esiVerify = httpRequest.request<EsiVerify>(
                        config.verifyUrl,
                        HttpMethod.Get,
                        null,
                        principal.accessToken
                    )
                }

                val character = getCharacter(esiVerify, config, httpRequest)
                if (character != null && esiVerify != null && principal != null) {
                    val session = call.sessions.get<Session>() ?: Session()
                    call.sessions.set(session.copy(
                        esiToken = EsiToken.Data(
                            accessToken = principal.accessToken,
                            refreshToken = principal.refreshToken.toString(),
                            expiresOn = esiVerify.ExpiresOn,
                            scopes = esiVerify.Scopes
                        ),
                        eveCharacter = character
                    ))
                }

                call.respondRedirect("/")
            }
        }
    }

    get("/api/auth/user") {
        val session = call.sessions.get<Session>()
        var data: ResponseAuthUser? = null
        if (session?.eveCharacter != null) {
            data = ResponseAuthUser(
                session.eveCharacter.name,
                session.eveCharacter.allianceName,
                session.eveCharacter.allianceTicker,
            )
        }
        call.respondText(gson.toJson(data), contentType = ContentType.Application.Json)
    }

    get("/api/auth/logout") {
        call.sessions.set(Session())
        call.respondText("")
    }
}

private suspend fun getCharacter(esiVerify: EsiVerify?, config: Config, httpRequest: HttpRequest): EveCharacter? {
    var eveCharacter: EveCharacter? = null

    if (esiVerify != null) {
        val esiAffiliation = httpRequest.post<Array<EsiAffiliation>>(
            "latest/characters/affiliation/?datasource=${config.esiDatasource}",
            "[${esiVerify.CharacterID}]"
        )
        val allianceId = esiAffiliation?.get(0)?.alliance_id

        if (allianceId != null) {
            val alliance = httpRequest.get<EsiAlliance>(
                "latest/alliances/$allianceId/?datasource=${config.esiDatasource}",
                "[${esiVerify.CharacterID}]"
            )

            if (alliance != null) {
                eveCharacter = EveCharacter(
                    esiVerify.CharacterID,
                    esiVerify.CharacterName,
                    allianceId,
                    alliance.name,
                    alliance.ticker
                )
            }
        }
    }

    return eveCharacter
}
