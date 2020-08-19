package net.tkhamez.everoute.routes

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import net.tkhamez.everoute.HttpRequest
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.Mongo
import net.tkhamez.everoute.data.*
import org.slf4j.Logger
import java.util.*

fun Route.gates(config: Config) {
        get("/gates/fetch") {
        val response = ResponseGates()
        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id

        if (allianceId == null) {
            response.message = "Failed to retrieve alliance of character."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        Mongo(config.db).gatesGet(allianceId).forEach { response.ansiblexes.add(it) }

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }

    get("/gates/update") {
        val response = ResponseGates()
        val characterId = call.sessions.get<Session>()?.esiVerify?.CharacterID
        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id

        val accessToken = EsiToken(config, call).get()
        if (accessToken == null || characterId == null || allianceId == null) {
            response.message = "Failed to retrieve alliance of character or ESI token."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)

        val alliance = mongo.allianceGet(allianceId)
        if (alliance?.updated != null && alliance.updated.time.plus((60 * 60 * 1000)) > Date().time) {
            response.message = "Gates were already updated within the last hour."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val gates = fetchGates(config, characterId, accessToken, call.application.environment.log)
        if (gates == null) {
            response.message = "ESI error."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        gates.forEach {
            mongo.gateStore(it, allianceId)
            response.ansiblexes.add(it)
        }
        mongo.gatesRemoveOther(gates, allianceId)
        mongo.allianceUpdate(Alliance(allianceId, Date()))

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }

    get("/gates/last-update") {
        val response = ResponseGatesUpdated()

        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id
        if (allianceId == null) {
            response.message = "Failed to retrieve alliance of character."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)
        val alliance = mongo.allianceGet(allianceId)
        response.allianceId = alliance?.id
        response.updated = alliance?.updated

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}

private suspend fun fetchGates(
    config: Config,
    characterId: Int,
    accessToken: String,
    log: Logger
): List<Ansiblex>? {
    val httpRequest = HttpRequest(config, log)

    // Find Ansiblexes with docking (deposit fuel) permission, needs scope esi-search.search_structures.v1
    val searchPath = "latest/characters/$characterId/search/"
    val searchParams = "?categories=structure&search=%20%C2%BB%20" // " » "
    val esiSearchStructure = httpRequest.get<EsiSearchStructure>(searchPath + searchParams, accessToken) ?: return null

    log.info("Got ${esiSearchStructure.structure.size} results.")

    // Fetch structure info, needs scope esi-universe.read_structures.v1
    val gates = mutableListOf<Ansiblex>()
    for (id in esiSearchStructure.structure) {
        val gate = httpRequest.get<EsiStructure>("latest/universe/structures/$id/", accessToken)
            ?: // TODO try again
            continue
        if (gate.type_id == 35841) {
            log.info("Fetched $id")
            gates.add(Ansiblex(id = id, name = gate.name, solarSystemId = gate.solar_system_id))
        } else {
            log.info("Not an Ansiblex $id")
        }
    }

    return gates
}
