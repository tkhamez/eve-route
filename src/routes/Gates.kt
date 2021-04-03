package net.tkhamez.everoute.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.routing.Route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import kotlinx.coroutines.launch
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.GraphHelper
import net.tkhamez.everoute.HttpRequest
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.database.DbInterface
import net.tkhamez.everoute.db
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

        val graphHelper = GraphHelper()
        db(config.db).gatesGet(allianceId).forEach {
            response.ansiblexes.add(Ansiblex(
                id = it.id,
                name = it.name,
                regionName = graphHelper.getGraph().regions[it.regionId] ?: "",
            ))
        }

        response.ansiblexes = response.ansiblexes
            .sortedWith(compareBy({ it.regionName }, { it.name }))
            .toMutableList()

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    post("/api/gates/delete/{ansiblexId}") {
        val response = ResponseMessage()

        val eveCharacter = call.sessions.get<Session>()?.eveCharacter
        if (eveCharacter?.allianceId == null || !eveCharacter.roles.contains(ROLE_IMPORT)) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val ansiblexId = call.parameters["ansiblexId"]?.toLong()
        if (ansiblexId !== null && db(config.db).gateDelete(ansiblexId, eveCharacter.allianceId)) {
            response.success = true
        }

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    post("/api/gates/search/{term}") {
        val log = call.application.environment.log
        val response = ResponseMessage()

        val searchTerm = call.parameters["term"].toString().trim()
        if (searchTerm != "»") {
            response.code = ResponseCodes.WrongSearchTerm
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val characterId = call.sessions.get<Session>()?.eveCharacter?.id
        val allianceId = call.sessions.get<Session>()?.eveCharacter?.allianceId
        val accessToken = EsiToken(config, call).get()
        if (accessToken == null || characterId == null || allianceId == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val db = db(config.db)
        val alliance = db.allianceGet(allianceId)
        if (alliance?.updated != null && alliance.updated.time.plus((60 * 60 * 1000)) > Date().time) {
            response.code = ResponseCodes.AlreadyUpdated
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
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
            db.allianceUpdate(MongoAlliance(id = allianceId, updated = Date()))

            // delete all "ESI" gates
            db.gatesRemoveSourceESI(allianceId)

            // fetch all gates in the background
            launch { fetchAndStoreGates(allianceId, esiSearchStructure.structure, accessToken, config, log, db) }
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

        val alliance = db(config.db).allianceGet(allianceId)
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
    log: Logger,
    db: DbInterface
) {
    val graphHelper = GraphHelper()
    val httpRequest = HttpRequest(config, log)

    val gates = mutableListOf<MongoAnsiblex>()
    val failed = mutableListOf<Long>()
    var fetched = 0
    for (id in structures) {
        val gate = httpRequest.get<EsiStructure>(
            "latest/universe/structures/$id/?datasource=${config.esiDatasource}",
            accessToken
        )
        if (gate == null) {
            failed.add(id) // TODO retry
            continue
        }
        if (gate.type_id != 35841) {
            continue
        }
        val system = graphHelper.findSystem(gate.solar_system_id) ?: continue
        fetched ++
        val ansiblex = MongoAnsiblex(
            id = id,
            name = gate.name,
            solarSystemId = gate.solar_system_id,
            regionId = system.regionId,
            source = MongoAnsiblex.Source.ESI,
        )
        gates.add(ansiblex)
        db.gateStore(ansiblex, allianceId)
    }

    log.info("Fetched and stored $fetched Ansiblexes.")
    if (failed.size > 0) {
        log.error("Failed to fetch ${failed.size} structures.")
    }
}
