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

fun Route.import(config: Config) {
    post("/api/import/from-game") {
        val response = ResponseMessage(success = false)

        val allianceId = call.sessions.get<Session>()?.eveCharacter?.allianceId
        if (allianceId == null) {
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val ansiblexes = parse(call.receiveText())

        if (ansiblexes.isNotEmpty()) {
            val regionId = ansiblexes[0].regionId
            if (regionId != null) {
                val db = db(config.db)
                db.allianceAdd(allianceId)
                db.gatesRemoveRegion(regionId, allianceId)
                for (ansiblex in ansiblexes) {
                    db.gateStore(ansiblex, allianceId)
                }
            }
        }

        response.success = true
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}

private fun parse(input: String): List<MongoAnsiblex> {
    val result = mutableListOf<MongoAnsiblex>()

    println(input)
    // TODO parse input into
    val data = mapOf(
        //1032197413122 to "3-OKDA » V2-VC2 - GO WEST",
        1033044488259 to "4M-HGL » KH0Z-0 - Staq &amp; Still",
    )

    val graphHelper = GraphHelper()

    for ((id, name) in data) {
        val startSystem = graphHelper.getStartSystem(name)

        // TODO find region and make sure all gates belong to the same region
        val regionId = 99

        if (startSystem != null) {
            result.add(MongoAnsiblex(
                id = id,
                name = name,
                solarSystemId = startSystem.id,
                regionId = regionId,
            ))
        }
    }

    return result
}
