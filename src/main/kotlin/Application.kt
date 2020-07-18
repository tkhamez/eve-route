package net.tkhamez.everoute

import io.ktor.application.*
import io.ktor.response.*
//import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
//import io.ktor.content.*
import io.ktor.http.content.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.ktor.auth.*
//import kotlinx.coroutines.*
import io.ktor.features.StatusPages
import java.io.File

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
fun main() {
    embeddedServer(
        Netty,
        watchPaths = listOf("classes"),
        port = 8080,
        module = Application::module
    ).start(true)
}

//@Suppress("unused") // Referenced in application.conf
//@kotlin.jvm.JvmOverloads
//fun Application.module(testing: Boolean = false) {
fun Application.module() {
    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respondText("Error.", status = HttpStatusCode.InternalServerError)
            throw cause
        }
        status(HttpStatusCode.NotFound, HttpStatusCode.Unauthorized) {
            call.respond(TextContent(
                    "${it.value} ${it.description}",
                    ContentType.Text.Plain.withCharset(Charsets.UTF_8),
                    it
            ))
        }
    }

    install(Sessions) {
        @Suppress("EXPERIMENTAL_API_USAGE")
        val storage = if (environment.config.property("eve.callback").getString().contains("localhost:8080")) {
            directorySessionStorage(File(System.getProperty("java.io.tmpdir") + "/.eve-route-sessions"), cached = true)
        } else {
            SessionStorageMemory()
        }
        cookie<Session>("EVE_ROUTE_SESSION", storage) {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
        }
    }

    install(Authentication) {
        oauth("eve-oauth") {
            client = httpClient
            providerLookup = {
                @Suppress("EXPERIMENTAL_API_USAGE")
                OAuthServerSettings.OAuth2ServerSettings(
                        name = "eve",
                        authorizeUrl = "https://login.eveonline.com/v2/oauth/authorize",
                        accessTokenUrl = "https://login.eveonline.com/v2/oauth/token",
                        requestMethod = HttpMethod.Post,
                        clientId = environment.config.property("eve.clientId").getString(),
                        clientSecret = environment.config.property("eve.clientSecret").getString(),
                        defaultScopes = listOf(
                                "esi-search.search_structures.v1",
                                "esi-universe.read_structures.v1",
                                "esi-location.read_location.v1",
                                "esi-ui.write_waypoint.v1"
                        )
                )
            }
            urlProvider = {
                @Suppress("EXPERIMENTAL_API_USAGE")
                environment.config.property("eve.callback").getString()
            }
        }
    }

    install(Routing) {
        frontend()
        authentication()
        findGates()
    }
}
