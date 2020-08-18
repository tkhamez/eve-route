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
        val session = call.sessions.get<Session>()
        val log = call.application.environment.log

        // logged in?
        if (session?.esiVerify == null || session.esiToken == null) {
            response.message = "Not logged in."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        // get/update access token
        val esiToken = EsiToken(config, log).getAccessToken(session.esiToken)
        call.sessions.set(session.copy(esiToken = esiToken))

        val gates = fetchGates(config.esiDomain, session.esiVerify, esiToken, log)
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
    esiVerify: EsiVerify,
    authToken: EsiToken.Data,
    log: Logger
): List<Ansiblex>? {
    // Find Ansiblexes with docking (deposit fuel) permission, needs scope esi-search.search_structures.v1
    val searchPath = "/latest/characters/${esiVerify.CharacterID}/search/"
    val searchParams = "?categories=structure&search=%20%C2%BB%20" // " » "
    val esiSearchStructure: EsiSearchStructure
    try {
        esiSearchStructure = httpClient.get(esiDomain + searchPath + searchParams) {
            header("Authorization", "Bearer ${authToken.accessToken}")
        }
    } catch (e: Exception) {
        log.error(e.message)
        return null
    }

    // Fetch structure info, needs scope esi-universe.read_structures.v1
    val gates = mutableListOf<Ansiblex>()
    for (id in esiSearchStructure.structure) {
        log.info("Fetching $id")
        var gate: EsiStructure? = null
        try {
            gate = httpClient.get("$esiDomain/latest/universe/structures/$id/") {
                header("Authorization", "Bearer ${authToken.accessToken}")
            }
        } catch (e: Exception) {
            // TODO try again
            log.error(e.message)
        }
        if (gate?.type_id == 35841) {
            gates.add(Ansiblex(id = id, name = gate.name, solarSystemId = gate.solar_system_id))
        }
    }

    return gates
}
