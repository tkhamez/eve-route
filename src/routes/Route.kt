package net.tkhamez.everoute.routes

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.EveRoute
import net.tkhamez.everoute.Mongo
import net.tkhamez.everoute.data.Config
import net.tkhamez.everoute.data.ResponseRouteCalculate
import net.tkhamez.everoute.data.ResponseRouteSet
import net.tkhamez.everoute.httpClient
import java.lang.Exception

fun Route.route(config: Config) {
    get("/route/calculate/{from}/{to}") {
        val gates = Mongo(config.db).getGates()
        val response = ResponseRouteCalculate()

        response.route = EveRoute(gates).find(
            call.parameters["from"].toString(),
            call.parameters["to"].toString()
        )

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }

    post("/route/set") {
        val response = ResponseRouteSet()

        val accessToken = EsiToken(config, call).get()
        if (accessToken == null) {
            response.message = "Not logged in or failed to retrieve ESI token."
            call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            return@post
        }

        val body = call.receiveText()
        val waypoints = Gson().fromJson(body, Array<EveRoute.Waypoint>::class.java)
        var incomingAnsiblex = false
        for ((index, value) in waypoints.withIndex()) {
            if (index != 0 && value.ansiblexId == null && ! incomingAnsiblex && index + 1 < waypoints.size) {
                continue
            }
            incomingAnsiblex = value.ansiblexId != null

            // Ansiblex ID = -1: there is a connection but ESI returned only the other gate
            val id = if (value.ansiblexId != null && value.ansiblexId > 0) {
                value.ansiblexId
            } else {
                value.systemId.toLong()
            }
            val path = "/latest/ui/autopilot/waypoint/"
            var params = "?destination_id=${id}" // system, station or structure’s id
            val clear = if (index > 0) "false" else "true"
            params += "&add_to_beginning=false&clear_other_waypoints=$clear"
            var result: HttpResponse? = null
            try {
                result = httpClient.post(config.esiDomain + path + params) {
                    header("Authorization", "Bearer $accessToken")
                }
            } catch (e: Exception) {
                call.application.environment.log.error(e.message)
            }
            if (result?.status != HttpStatusCode.NoContent) {
                response.message = "Error: ${result?.status}"
            }
        }

        response.message = if (response.message.isEmpty()) "Success." else response.message
        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}
