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
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.Mongo
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.httpClient
import org.slf4j.Logger
import java.lang.Exception

fun Route.gates(config: Config) {
    get("/gates/fetch") {
        val response = ResponseGates(success = true)

        Mongo(config.db).getGates().forEach { response.ansiblexes.add(it) }

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }

    get("/gates/update") {
        val response = ResponseGates()
        val characterId = call.sessions.get<Session>()?.esiVerify?.CharacterID

        val accessToken = EsiToken(config, call).get()
        if (accessToken == null || characterId == null) {
            response.message = "Not logged in or failed to retrieve ESI token."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val gates = fetchGates(config.esiDomain, characterId, accessToken, call.application.environment.log)
        if (gates == null) {
            response.message = "ESI error."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)

        gates.forEach {
            mongo.storeGate(it)
            response.ansiblexes.add(it)
        }
        mongo.removeOtherGates(gates)

        response.success = true
        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}

private suspend fun fetchGates(
    esiDomain: String,
    characterId: Long,
    accessToken: String,
    log: Logger
): List<Ansiblex>? {
    // Find Ansiblexes with docking (deposit fuel) permission, needs scope esi-search.search_structures.v1
    val searchPath = "/latest/characters/$characterId/search/"
    val searchParams = "?categories=structure&search=%20%C2%BB%20" // " » "
    val esiSearchStructure: EsiSearchStructure
    try {
        esiSearchStructure = httpClient.get(esiDomain + searchPath + searchParams) {
            header("Authorization", "Bearer $accessToken")
        }
    } catch (e: Exception) {
        log.error(e.message)
        return null
    }
    log.info("Got ${esiSearchStructure.structure.size} results.")

    // Fetch structure info, needs scope esi-universe.read_structures.v1
    val gates = mutableListOf<Ansiblex>()
    for (id in esiSearchStructure.structure) {
        var gate: EsiStructure? = null
        try {
            gate = httpClient.get("$esiDomain/latest/universe/structures/$id/") {
                header("Authorization", "Bearer $accessToken")
            }
        } catch (e: Exception) {
            // TODO try again
            log.error(e.message)
        }
        if (gate?.type_id == 35841) {
            log.info("Fetched $id")
            gates.add(Ansiblex(id = id, name = gate.name, solarSystemId = gate.solar_system_id))
        } else {
            log.info("Not an Ansiblex $id")
        }
    }

    return gates
}
