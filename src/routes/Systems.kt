package net.tkhamez.everoute.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.*
import io.ktor.routing.*
import net.tkhamez.everoute.GraphHelper
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.gson

fun Route.systems() {
    get("/api/systems/find/{search}") {
        val search = call.parameters["search"].toString().trim()

        val listA: MutableList<String> = mutableListOf()
        val listB: MutableList<String> = mutableListOf()
        val graph = GraphHelper().getGraph()
        graph.systems.forEach {
            if (search == "" || it.name.toLowerCase().contains(search.toLowerCase())) {
                val name = "${it.name} - ${graph.regions[it.regionId]} (${it.security})"
                if (it.name.startsWith(search, true)) {
                    listA.add(name)
                } else {
                    listB.add(name)
                }
            }
        }

        listA.sort()
        listB.sort()

        val response = ResponseSystemNames()
        response.systems.addAll(listA)
        response.systems.addAll(listB)

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}
