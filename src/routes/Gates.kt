package net.tkhamez.everoute.routes

import com.google.gson.Gson
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
import org.slf4j.Logger
import java.util.*

fun Route.gates(config: Config) {
        get("/api/gates/fetch") {
        val response = ResponseGates()
        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id

        if (allianceId == null) {
            response.code = ResponseCodes.AuthAllianceFail
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        Mongo(config.db).gatesGet(allianceId).forEach { response.ansiblexes.add(it) }

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/gates/search/{term}") {
        val log = call.application.environment.log
        val response = ResponseMessage()

        val searchTerm = call.parameters["term"].toString().trim()
        if (searchTerm != "»") {
            response.code = ResponseCodes.WrongSearchTerm
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val characterId = call.sessions.get<Session>()?.esiVerify?.CharacterID
        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id
        val accessToken = EsiToken(config, call).get()
        if (accessToken == null || characterId == null || allianceId == null) {
            response.code = ResponseCodes.AuthAllianceOrTokenFail
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)
        val alliance = mongo.allianceGet(allianceId)
        if (alliance?.updated != null && alliance.updated.time.plus((60 * 60 * 1000)) > Date().time) {
            response.code = ResponseCodes.AlreadyUpdated
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
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

            // fetch all gates in the background
            launch { fetchAndStoreGates(allianceId, esiSearchStructure.structure, accessToken, config, log) }
        }

        // Set update date now to prevent a second parallel update.
        mongo.allianceUpdate(Alliance(allianceId, Date()))

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/gates/last-update") {
        val response = ResponseGatesUpdated()

        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id
        if (allianceId == null) {
            response.code = ResponseCodes.AuthAllianceFail
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

private suspend fun fetchAndStoreGates(
    allianceId: Int,
    structures: List<Long>,
    accessToken: String,
    config: Config,
    log: Logger
) {
    val httpRequest = HttpRequest(config, log)
    val mongo = Mongo(config.db)

    val gates = mutableListOf<Ansiblex>()
    val failed = mutableListOf<Long>()
    var fetched = 0
    for (id in structures) {
        val gate = httpRequest.get<EsiStructure>(
            "latest/universe/structures/$id/?datasource=${config.esiDatasource}",
            accessToken
        )
        if (gate == null) {
            // TODO try again
            failed.add(id)
            continue
        }
        if (gate.type_id == 35841) {
            log.info("Fetched $id")
            fetched ++
            val ansiblex = Ansiblex(id = id, name = gate.name, solarSystemId = gate.solar_system_id)
            gates.add(ansiblex)
            mongo.gateStore(ansiblex, allianceId)
        }
    }

    mongo.gatesRemoveOther(gates, allianceId)

    log.info("Fetched and stored $fetched Ansiblexes, ${failed.size} requests failed.")
}
