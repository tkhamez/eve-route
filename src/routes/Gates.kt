package net.tkhamez.everoute.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import kotlinx.coroutines.launch
import net.tkhamez.everoute.HttpRequest
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.Mongo
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.gson
import org.slf4j.Logger
import java.util.*

fun Route.gates(config: Config) {
        get("/api/gates/fetch") {
        val response = ResponseGates()
        val allianceId = call.sessions.get<Session>()?.eveCharacter?.allianceId

        if (allianceId == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        Mongo(config.db).gatesGet(allianceId).forEach { response.ansiblexes.add(it) }

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/gates/search/{term}") {
        val log = call.application.environment.log
        val response = ResponseMessage()

        val searchTerm = call.parameters["term"].toString().trim()
        if (searchTerm != "»") {
            response.code = ResponseCodes.WrongSearchTerm
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val characterId = call.sessions.get<Session>()?.eveCharacter?.id
        val allianceId = call.sessions.get<Session>()?.eveCharacter?.allianceId
        val accessToken = EsiToken(config, call).get()
        if (accessToken == null || characterId == null || allianceId == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)
        val alliance = mongo.allianceGet(allianceId)
        if (alliance?.updated != null && alliance.updated.time.plus((60 * 60 * 1000)) > Date().time) {
            response.code = ResponseCodes.AlreadyUpdated
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        // Find Ansiblexes with docking (deposit fuel) permission, needs scope esi-search.search_structures.v1
        val httpRequest = HttpRequest(config, log)
        val searchPath = "latest/characters/$characterId/search/?datasource=${config.esiDatasource}"
        val searchParams = "&categories=structure&search=%20%C2%BB%20" // " » "
        val esiSearchStructure = httpRequest.get<EsiSearchStructure>(searchPath + searchParams, accessToken)
        if (esiSearchStructure == null) {
            response.code = ResponseCodes.SearchError
        } else {
            response.code = ResponseCodes.SearchSuccess
            response.param = esiSearchStructure.structure.size.toString()

            // Set update date now to prevent a second parallel update.
            mongo.allianceUpdate(MongoAlliance(id = allianceId, updated = Date()))

            // fetch all gates in the background
            launch { fetchAndStoreGates(allianceId, esiSearchStructure.structure, accessToken, config, log) }
        }

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/gates/last-update") {
        val response = ResponseGatesUpdated()

        val allianceId = call.sessions.get<Session>()?.eveCharacter?.allianceId
        if (allianceId == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)
        val alliance = mongo.allianceGet(allianceId)
        response.allianceId = alliance?.id
        response.updated = alliance?.updated

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}

private suspend fun fetchAndStoreGates(
    allianceId: Int,
    structures: List<Long>,
    accessToken: String,
    config: Config,
    log: Logger
) {
    val httpRequest = HttpRequest(config, log)
    val mongo = Mongo(config.db)

    val gates = mutableListOf<MongoAnsiblex>()
    val failed = mutableListOf<Long>()
    var fetched = 0
    for (id in structures) {
        val gate = httpRequest.get<EsiStructure>(
            "latest/universe/structures/$id/?datasource=${config.esiDatasource}",
            accessToken
        )
        if (gate == null) {
            failed.add(id)
            continue
        }
        if (gate.type_id == 35841) {
            fetched ++
            val ansiblex = MongoAnsiblex(
                id = id,
                allianceId = allianceId,
                name = gate.name,
                solarSystemId = gate.solar_system_id,
            )
            gates.add(ansiblex)
            mongo.gateStore(ansiblex, allianceId)
        }
    }

    mongo.gatesRemoveOther(gates, allianceId)

    log.info("Fetched and stored $fetched Ansiblexes.")
    if (failed.size > 0) {
        log.error("Failed to fetch ${failed.size} structures.")
    }
}
