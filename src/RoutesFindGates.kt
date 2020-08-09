package net.tkhamez.everoute

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import java.lang.Exception

fun Route.findGates(config: Config) {
    get("/find-gates") {
        val response = ResponseFindGates()
        val session = call.sessions.get<Session>()
        val esiDomain = "https://esi.evetech.net"

        // logged in?
        if (session?.esiVerify == null || session.authToken == null) {
            response.message = "Not logged in."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        // get/update access token
        val authToken = Token(config).getAccessToken(session.authToken)
        call.sessions.set(session.copy(authToken = authToken))

        // Find Ansiblexes with docking (deposit fuel) permission, needs scope esi-search.search_structures.v1
        val searchPath = "/latest/characters/${session.esiVerify.CharacterID}/search/"
        val searchParams = "?categories=structure&search=%20%C2%BB%20" // " » "
        val gates: EsiSearchStructure
        try {
            gates = httpClient.get(esiDomain + searchPath + searchParams) {
                header("Authorization", "Bearer ${authToken.accessToken}")
            }
        } catch(e: Exception) {
            response.message = e.message.toString()
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        // Fetch structure info, needs scope esi-universe.read_structures.v1
        for (id in gates.structure) {
            var gate: EsiStructure? = null
            try {
                gate = httpClient.get("$esiDomain/latest/universe/structures/$id/") {
                    header("Authorization", "Bearer ${authToken.accessToken}")
                }
            } catch(e: Exception) {
                println(e.message)
            }
            if (gate?.type_id == 35841) {
                response.gates.add(gate)
            }
        }

        response.success = true
        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}
