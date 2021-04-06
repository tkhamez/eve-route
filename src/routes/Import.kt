package net.tkhamez.everoute.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.*
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.sessions.*
import net.tkhamez.everoute.GraphHelper
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.db
import net.tkhamez.everoute.gson
import org.slf4j.Logger

private const val IMPORT_MODE_REPLACE_REGION = "replace-region"
private const val IMPORT_MODE_ADD_GATES = "add-gates"

fun Route.import(config: Config) {
    post("/api/import/from-game") {
        val response = ResponseMessage(success = false)

        val eveCharacter = call.sessions.get<Session>()?.eveCharacter
        if (eveCharacter?.allianceId == null || !eveCharacter.roles.contains(ROLE_IMPORT)) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val mode = call.parameters["mode"].toString()
        val ansiblexes = parse(call.receiveText(), mode, call.application.environment.log)
        val numImported = import(ansiblexes, config.db, eveCharacter.allianceId, mode)

        response.success = true
        response.code = ResponseCodes.ImportedGates
        response.param = numImported.toString()
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}

private fun parse(input: String, mode: String, log: Logger): List<MongoAnsiblex> {
    // parse input
    val parsedInput = mutableMapOf<Long, String>()
    // Tag is e.g. <a href="showinfo:35841//1034848981067">C9N-CC » 6EK-BV - one more</a>
    val tags = Regex("(?i)<a href=\"showinfo:35841//([^>]+)\">(.+?)</a>", setOf(RegexOption.IGNORE_CASE))
    for (matchedTag in tags.findAll(input)) {
        if (
            matchedTag.groupValues.size == 3 &&
            matchedTag.groupValues[2].contains(" » ") &&
            matchedTag.groupValues[2].contains(" - ")
        ) {
            // Ansiblex ID = Ansiblex name
            parsedInput[matchedTag.groupValues[1].toLong()] = matchedTag.groupValues[2]
        }
    }

    // build result
    val result = mutableListOf<MongoAnsiblex>()
    val graphHelper = GraphHelper()
    var regionId :Int? = null
    for ((id, name) in parsedInput) {
        val startSystem = graphHelper.getStartSystem(name)
        val endSystem = graphHelper.getEndSystem(name)
        if (startSystem == null || endSystem == null) {
            log.error("Error: Could not find start and/or end system for Ansiblex \"$name\"")
            continue
        }
        if (regionId == null) {
            regionId = startSystem.regionId
        }
        if (mode == IMPORT_MODE_REPLACE_REGION && startSystem.regionId != regionId) {
            log.error("Error: System \"${startSystem.name}\" in another region.")
            continue
        }
        result.add(MongoAnsiblex(
            id = id,
            name = name,
            solarSystemId = startSystem.id,
            regionId = regionId,
            source = MongoAnsiblex.Source.Import,
        ))
    }

    return result
}

private fun import(ansiblexes: List<MongoAnsiblex>, dbConfig: String, allianceId: Int, mode: String): Int {
    if (ansiblexes.isEmpty()) {
        return 0
    }

    val db = db(dbConfig)
    db.allianceAdd(allianceId)

    var add = false
    if (mode == IMPORT_MODE_REPLACE_REGION) {
        val regionId = ansiblexes[0].regionId
        if (regionId != null) {
            db.gatesRemoveRegion(regionId, allianceId)
            add = true
        }
    } else if (mode == IMPORT_MODE_ADD_GATES) {
        add = true
    }

    var numImported = 0
    if (add) {
        for (ansiblex in ansiblexes) {
            db.gateStore(ansiblex, allianceId)
            numImported ++
        }
    }

    return numImported
}
