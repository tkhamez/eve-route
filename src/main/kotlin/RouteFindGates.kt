package net.tkhamez.everoute

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.findGates() {

    get("/find-gates") {
        // finds Ansiblexes with docking (deposit fuel) permission
        // scope: esi-search.search_structures.v1
        // /latest/characters/{character_id}/search/?categories=structure&search= »
        // result: [12314423323,234243234234]

        // /latest/universe/structures/{structure_id}/
        // esi-universe.read_structures.v1
        // result: "name": "a", "owner_id": 1, "position": {"x": 4, "y": 2, "z": 3}, "solar_system_id": 1, "type_id": 3

        var data = emptyMap<String, Any?>()
        call.respondText(Gson().toJson(data), contentType = ContentType.Application.Json)
    }

}