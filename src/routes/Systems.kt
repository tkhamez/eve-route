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
        GraphHelper().getGraph().systems.forEach {
            if (search == "" || it.name.toLowerCase().contains(search.toLowerCase())) {
                if (it.name.startsWith(search, true)) {
                    listA.add(it.name)
                } else {
                    listB.add(it.name)
                }
            }
        }

        listA.sort();
        listB.sort();

        val response = ResponseSystemNames()
        response.systems.addAll(listA)
        response.systems.addAll(listB)

        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }
}
