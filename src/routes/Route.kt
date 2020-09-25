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
import io.ktor.sessions.*
import net.tkhamez.everoute.*
import net.tkhamez.everoute.Route as EveRoute
import net.tkhamez.everoute.GraphHelper
import net.tkhamez.everoute.data.*

fun Route.route(config: Config) {
    get("/api/route/location") {
        val response = ResponseRouteLocation()

        val characterId = call.sessions.get<Session>()?.eveCharacter?.id
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
            response.solarSystemName = GraphHelper().findSystem(response.solarSystemId!!)?.name
        }

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    /**
     * Returns all Ansiblex and temporary connections (system names)
     */
    get("/api/route/map-connections") {
        val response = ResponseMapConnections()

        val session = call.sessions.get<Session>()
        val allianceId = session?.eveCharacter?.allianceId
        val characterId = session?.eveCharacter?.id

        if (allianceId == null || characterId == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val db = db(config.db)
        val gates = db.gatesGet(allianceId)
        val tempConnections = db.temporaryConnectionsGet(characterId)

        val graphHelper = GraphHelper()

        for (connectionType in listOf(gates, tempConnections)) {
            val addedConnections = mutableListOf<String>()
            connectionType.forEach { connection ->
                var startSystem: GraphSystem? = null
                var endSystem: GraphSystem? = null
                if (connection is MongoAnsiblex) {
                    startSystem = graphHelper.findSystem(connection.solarSystemId)
                    endSystem = graphHelper.getEndSystem(connection)
                } else if (connection is MongoTemporaryConnection) {
                    startSystem = graphHelper.findSystem(connection.system1Id)
                    endSystem = graphHelper.findSystem(connection.system2Id)
                }
                if (
                    startSystem != null &&
                    endSystem != null &&
                    !addedConnections.contains("${startSystem.name} - ${endSystem.name}")
                ) {
                    addedConnections.add("${startSystem.name} - ${endSystem.name}")
                    addedConnections.add("${endSystem.name} - ${startSystem.name}")
                    if (connection is MongoAnsiblex) {
                        response.ansiblexes.add(ConnectedSystems(startSystem.name, endSystem.name))
                    } else { // MongoTemporaryConnection
                        response.temporary.add(ConnectedSystems(startSystem.name, endSystem.name))
                    }
                }
            }
        }

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/route/avoided-systems") {
        val response = ResponseSystems()
        val avoidedSystems = call.sessions.get<Session>()?.avoidedSystems ?: mutableSetOf()
        val graphHelper = GraphHelper()
        avoidedSystems.forEach {
            val system = graphHelper.findSystem(it)
            if (system != null) {
                response.systems.add(System(system.id, system.name))
            }
        }
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/route/removed-connections") {
        val response = ResponseConnectedSystems()
        val removedConnections = call.sessions.get<Session>()?.removedConnections ?: mutableSetOf()
        val graphHelper = GraphHelper()
        removedConnections.forEach {
            val system1 = graphHelper.findSystem(it.system1)
            val system2 = graphHelper.findSystem(it.system2)
            if (system1 != null && system2 != null) {
                response.connections.add(ConnectedSystems(system1.name, system2.name))
            }
        }
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    post("/api/route/avoid-system/{id}") {
        val session = call.sessions.get<Session>()
        val avoidedSystems = session?.avoidedSystems ?: mutableSetOf()
        val systemId = call.parameters["id"]?.toInt()
        if (systemId != null) {
            avoidedSystems.add(systemId)
            call.sessions.set(session?.copy(avoidedSystems = avoidedSystems))
        }
        call.respondText("", contentType = ContentType.Application.Json)
    }

    post("/api/route/remove-connection/{from}/{to}") {
        val session = call.sessions.get<Session>()
        val removedConnections = session?.removedConnections ?: mutableSetOf()
        val from = call.parameters["from"]
        val to = call.parameters["to"]
        if (from != null && to != null) {
            removedConnections.add(ConnectedSystems(from, to))
            call.sessions.set(session?.copy(removedConnections = removedConnections))
        }
        call.respondText("", contentType = ContentType.Application.Json)
    }

    post("/api/route/reset-avoided-system-and-removed-connection") {
        call.sessions.set(call.sessions.get<Session>()?.copy(removedConnections = mutableSetOf()))
        call.sessions.set(call.sessions.get<Session>()?.copy(avoidedSystems = mutableSetOf()))
        call.respondText("", contentType = ContentType.Application.Json)
    }

    get("/api/route/find/{from}/{to}") {
        val response = ResponseRouteFind()
        val session = call.sessions.get<Session>()
        val allianceId = session?.eveCharacter?.allianceId
        val characterId = session?.eveCharacter?.id

        if (allianceId == null || characterId == null) {
            response.code = ResponseCodes.AuthError
            call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
            return@get
        }

        val db = db(config.db)
        val gates = db.gatesGet(allianceId)
        db.temporaryConnectionsDeleteAllExpired()
        val tempConnections = db.temporaryConnectionsGet(characterId)
        val avoidedSystems = session.avoidedSystems ?: mutableSetOf()
        val removedConnections = session.removedConnections ?: mutableSetOf()
        response.route = EveRoute(gates, tempConnections, avoidedSystems, removedConnections).find(
            call.parameters["from"].toString().trim(),
            call.parameters["to"].toString().trim()
        )

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    post("/api/route/set") {
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
        var incomingTemporary = false
        var startSystemSet = false
        for ((index, value) in waypoints.withIndex()) {
            if (
                value.wormhole // can't set waypoint to a wormhole
                ||
                (
                    startSystemSet && // not start system
                    index + 1 < waypoints.size && // not end system
                    value.ansiblexId == null && // not an ansiblex
                    value.connectionType != EveRoute.Waypoint.Type.Temporary && // not a temporary connection
                    !incomingTemporary // previous system was not a temporary connection
                )
            ) {
                continue
            }
            incomingTemporary = value.connectionType == EveRoute.Waypoint.Type.Temporary

            // Ansiblex ID = -1: there is a connection but ESI returned only the other gate
            val id = if (value.ansiblexId != null && value.ansiblexId > 0) {
                value.ansiblexId
            } else {
                value.systemId.toLong()
            }
            val path = "latest/ui/autopilot/waypoint/?datasource=${config.esiDatasource}"
            var params = "&destination_id=${id}" // system, station or structure’s id
            val clear = if (startSystemSet) "false" else "true"
            params += "&add_to_beginning=false&clear_other_waypoints=$clear"
            val result = httpRequest.post<HttpResponse>(path + params, null, accessToken)
            if (result?.status != HttpStatusCode.NoContent) {
                response.param = result?.status.toString()
            }
            startSystemSet = true
        }

        response.code = if (response.param == null)
            ResponseCodes.SetWaypointsSuccess
        else
            ResponseCodes.SetWaypointsFailure
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}
