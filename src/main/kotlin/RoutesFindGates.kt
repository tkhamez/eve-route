package net.tkhamez.everoute

import com.google.gson.Gson
import io.ktor.application.ApplicationEnvironment
import io.ktor.application.call
import io.ktor.client.features.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set

fun Route.findGates(environment: ApplicationEnvironment) {
    get("/find-gates") {
        val response = ResponseFindGates()
        val session = call.sessions.get<Session>()
        val esiDomain = "https://esi.evetech.net"

        if (session == null || ! session.eveAuth.containsKey("id")) {
            response.message = "Not logged in."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        @Suppress("EXPERIMENTAL_API_USAGE")
        val token = Token(
                environment.config.property("eve.accessTokenUrl").getString(),
                environment.config.property("eve.clientId").getString(),
                environment.config.property("eve.clientSecret").getString()
        ).getAccessToken(session.eveAuth)

        // TODO find a better way
        call.sessions.set(session.copy(eveAuth = mapOf(
                "id" to session.eveAuth["id"],
                "name" to session.eveAuth["name"],
                "accessToken" to token.access_token,
                "scopes" to session.eveAuth["scopes"],
                "expiresOn" to token.expiresOn,
                "refreshToken" to session.eveAuth["refreshToken"]
        )))

        // Find Ansiblexes with docking (deposit fuel) permission, needs scope esi-search.search_structures.v1
        val searchPath = "/latest/characters/${session.eveAuth["id"]}/search/"
        val searchParams = "?categories=structure&search=%20%C2%BB%20" // " » "
        val gates: EsiSearchStructure
        try {
            gates = httpClient.get(esiDomain + searchPath + searchParams) {
                header("Authorization", "Bearer ${token.access_token}")
            }
        } catch(e: ResponseException) {
            response.message = e.message.toString()
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        // Fetch structure info, needs scope esi-universe.read_structures.v1
        for (id in gates.structure) {
            var gate = EsiStructure()
            try {
                gate = httpClient.get("$esiDomain/latest/universe/structures/$id/") {
                    header("Authorization", "Bearer ${token.access_token}")
                }
            } catch(e: ResponseException) {
                println(e.message)
            }
            if (gate.type_id == 35841) {
                response.gates.add(gate)
            }
        }

        response.success = true
        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}
