package net.tkhamez.everoute.routes

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
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.Mongo
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.httpClient
import java.lang.Exception

fun Route.getGates(config: Config) {
    get("/get-gates") {
        val response = ResponseGates(success = true)

        Mongo(config.db).getGates().forEach { response.gates.add(it) }

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}

fun Route.updateGates(config: Config) {
    get("/update-gates") {
        val response = ResponseGates()
        val session = call.sessions.get<Session>()

        // logged in?
        if (session?.esiVerify == null || session.esiToken == null) {
            response.message = "Not logged in."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        // get/update access token
        val esiToken = EsiToken(config).getAccessToken(session.esiToken)
        call.sessions.set(session.copy(esiToken = esiToken))

        val gates = fetchGates(config.esiDomain, session.esiVerify, esiToken)
        if (gates == null) {
            response.message = "ESI error."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)

        gates.forEach {
            mongo.storeGate(it)
            response.gates.add(it)
        }

        response.success = true
        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}

private suspend fun fetchGates(esiDomain: String, esiVerify: EsiVerify, authToken: EsiToken.Data): List<Gate>? {

    // Find Ansiblexes with docking (deposit fuel) permission, needs scope esi-search.search_structures.v1
    val searchPath = "/latest/characters/${esiVerify.CharacterID}/search/"
    val searchParams = "?categories=structure&search=%20%C2%BB%20" // " » "
    val esiSearchStructure: EsiSearchStructure
    try {
        esiSearchStructure = httpClient.get(esiDomain + searchPath + searchParams) {
            header("Authorization", "Bearer ${authToken.accessToken}")
        }
    } catch(e: Exception) {
        return null
    }

    // Fetch structure info, needs scope esi-universe.read_structures.v1
    val gates = mutableListOf<Gate>()
    for (id in esiSearchStructure.structure) {
        var gate: EsiStructure? = null
        try {
            gate = httpClient.get("$esiDomain/latest/universe/structures/$id/") {
                header("Authorization", "Bearer ${authToken.accessToken}")
            }
        } catch(e: Exception) {
            // TODO try again
            println(e.message)
        }
        if (gate?.type_id == 35841) {
            gates.add(Gate(id = id, name = gate.name, solarSystemId = gate.solar_system_id))
        }
    }

    return gates
}
