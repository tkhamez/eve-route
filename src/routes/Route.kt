package net.tkhamez.everoute.routes

import com.google.gson.Gson
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
import net.tkhamez.everoute.data.Config
import net.tkhamez.everoute.data.ResponseRouteCalculate
import net.tkhamez.everoute.data.ResponseRouteSet
import net.tkhamez.everoute.data.Session

fun Route.route(config: Config) {
    get("/route/calculate/{from}/{to}") {
        val response = ResponseRouteCalculate()
        val allianceId = call.sessions.get<Session>()?.esiAffiliation?.alliance_id

        if (allianceId == null) {
            response.message = "Failed to retrieve alliance of character or ESI token."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val gates = Mongo(config.db).gatesGet(allianceId)
        response.route = EveRoute(gates).find(
            call.parameters["from"].toString(),
            call.parameters["to"].toString()
        )

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }

    post("/route/set") {
        // This is not needed, but can make the route better visible in the in-game map
        val setAnsiblexExitSystemAsWaypoint = false

        val response = ResponseRouteSet()

        val accessToken = EsiToken(config, call).get()
        if (accessToken == null) {
            response.message = "Not logged in or failed to retrieve ESI token."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val httpRequest = HttpRequest(config, call.application.environment.log)
        val body = call.receiveText()
        val waypoints = Gson().fromJson(body, Array<EveRoute.Waypoint>::class.java)
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
            val path = "latest/ui/autopilot/waypoint/"
            var params = "?destination_id=${id}" // system, station or structure’s id
            val clear = if (index > 0) "false" else "true"
            params += "&add_to_beginning=false&clear_other_waypoints=$clear"
            val result = httpRequest.post<HttpResponse>(path + params, null, accessToken)
            if (result?.status != HttpStatusCode.NoContent) {
                response.message = "Error: ${result?.status}"
            }
        }

        response.message = if (response.message.isEmpty()) "Success." else response.message
        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}
