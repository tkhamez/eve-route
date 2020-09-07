package net.tkhamez.everoute.routes

import io.ktor.application.call
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import net.tkhamez.everoute.*
import net.tkhamez.everoute.Graph
import net.tkhamez.everoute.data.*

fun Route.route(config: Config) {
    get("/api/route/location") {
        val response = ResponseRouteLocation()

        val characterId = call.sessions.get<Session>()?.esiVerify?.CharacterID
        val accessToken = EsiToken(config, call).get()
        if (characterId == null || accessToken == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val httpRequest = HttpRequest(config, call.application.environment.log)
        val location = httpRequest.get<EsiLocation>("latest/characters/$characterId/location/", accessToken)
        response.solarSystemId = location?.solar_system_id
        if (response.solarSystemId != null) {
            response.solarSystemName = Graph().findSystem(response.solarSystemId!!)?.name
        }

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/route/find/{from}/{to}") {
        val response = ResponseRouteFind()
        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id
        val characterId = call.sessions.get<Session>()?.esiVerify?.CharacterID

        if (allianceId == null || characterId == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val mongo = Mongo(config.db)
        val gates = mongo.gatesGet(allianceId)
        mongo.temporaryConnectionsDeleteAllExpired()
        val tempConnections = mongo.temporaryConnectionsGet(characterId)
        response.route = EveRoute(gates, tempConnections).find(
            call.parameters["from"].toString().trim(),
            call.parameters["to"].toString().trim()
        )

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    post("/api/route/set") {
        // This is not needed, but can make the route better visible in the in-game map
        val setAnsiblexExitSystemAsWaypoint = false

        val response = ResponseMessage()

        val accessToken = EsiToken(config, call).get()
        if (accessToken == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val httpRequest = HttpRequest(config, call.application.environment.log)
        val body = call.receiveText()
        val waypoints = gson.fromJson(body, Array<EveRoute.Waypoint>::class.java)
        var incomingAnsiblex = false
        for ((index, value) in waypoints.withIndex()) {
            if (
                index != 0 && // not start system
                index + 1 < waypoints.size && // not end system
                value.ansiblexId == null && // not an ansiblex
                (! setAnsiblexExitSystemAsWaypoint || ! incomingAnsiblex)
            ) {
                continue
            }
            incomingAnsiblex = value.ansiblexId != null

            // Ansiblex ID = -1: there is a connection but ESI returned only the other gate
            val id = if (value.ansiblexId != null && value.ansiblexId > 0) {
                value.ansiblexId
            } else {
                value.systemId.toLong()
            }
            val path = "latest/ui/autopilot/waypoint/?datasource=${config.esiDatasource}"
            var params = "&destination_id=${id}" // system, station or structure’s id
            val clear = if (index > 0) "false" else "true"
            params += "&add_to_beginning=false&clear_other_waypoints=$clear"
            val result = httpRequest.post<HttpResponse>(path + params, null, accessToken)
            if (result?.status != HttpStatusCode.NoContent) {
                response.param = result?.status.toString()
            }
        }

        response.code = if (response.param == null) ResponseCodes.Success else ResponseCodes.Error
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}
