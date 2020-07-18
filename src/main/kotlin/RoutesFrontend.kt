package net.tkhamez.everoute

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.frontend() {
    get("/") {
        val resource = javaClass.getResource("/public/index.html")
        if (resource != null) {
            call.respondText(resource.readText(), contentType = ContentType.Text.Html)
        } else {
            call.respondText("File not found.")
        }
    }

    static("/") {
        resources("public")
    }
}
