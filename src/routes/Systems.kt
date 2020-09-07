package net.tkhamez.everoute.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.*
import io.ktor.routing.*
import net.tkhamez.everoute.Graph
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.gson

fun Route.systems() {
    get("/api/systems/find/{search}") {
        val response = getResponse(call.parameters["search"].toString().trim())

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/systems/fetch") {
        val response = getResponse("")

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}

private fun getResponse(search: String): ResponseSystems {
    val graph = Graph().getSystems()

    val response = ResponseSystems()
    graph.systems.forEach {
        if (search == "" || it.name.toLowerCase().contains(search.toLowerCase())) {
            response.systems.add(it.name)
        }
    }
    response.systems.sort()

    return response
}
