package net.tkhamez.everoute.routes

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import net.tkhamez.everoute.EveRoute
import net.tkhamez.everoute.data.ResponseCalculate

fun Route.calculate() {
    get("/calculate/{from}/{to}") {
        val response = ResponseCalculate()
        response.route = EveRoute().find(
            call.parameters["from"].toString(),
            call.parameters["to"].toString()
        )

        call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
    }
}
