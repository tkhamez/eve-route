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

fun Route.import(config: Config) {
    post("/api/import/from-game") {
        val response = ResponseMessage(success = false)

        val eveCharacter = call.sessions.get<Session>()?.eveCharacter
        if (eveCharacter?.allianceId == null || !eveCharacter.roles.contains("import")) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val ansiblexes = parse(call.receiveText(), call.application.environment.log)

        var numImported = 0
        if (ansiblexes.isNotEmpty()) {
            val regionId = ansiblexes[0].regionId
            if (regionId != null) {
                val db = db(config.db)
                db.allianceAdd(eveCharacter.allianceId)
                db.gatesRemoveRegion(regionId, eveCharacter.allianceId)
                for (ansiblex in ansiblexes) {
                    db.gateStore(ansiblex, eveCharacter.allianceId)
                    numImported ++
                }
            }
        }

        response.success = true
        response.code = ResponseCodes.ImportedGates
        response.param = numImported.toString()
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}

private fun parse(input: String, log: Logger): List<MongoAnsiblex> {
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
        if (startSystem.regionId != regionId) {
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
