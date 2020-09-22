package net.tkhamez.everoute

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.ktor.application.*
import io.ktor.response.*
//import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
//import io.ktor.content.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.ktor.auth.*
import io.ktor.features.*
//import kotlinx.coroutines.*
import io.ktor.util.KtorExperimentalAPI
import net.tkhamez.everoute.data.Config
import net.tkhamez.everoute.data.Session
import net.tkhamez.everoute.routes.*
import org.slf4j.LoggerFactory
import java.io.File

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
fun main() {
    embeddedServer(
        Netty,
        watchPaths = listOf("classes"),
        port = 8080,
        module = Application::module
    ).start(true)
}

@KtorExperimentalLocationsAPI
@Location("/api/auth/login/{features?}") class Login(val features: String = "")

//@Suppress("unused") // Referenced in application.conf
//@kotlin.jvm.JvmOverloads
//fun Application.module(testing: Boolean = false) {
@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
fun Application.module() {

    val config = Config(
        db = environment.config.property("app.db").getString(),
        clientId = environment.config.property("app.clientId").getString(),
        clientSecret = environment.config.property("app.clientSecret").getString(),
        callback = environment.config.property("app.callback").getString(),
        authorizeUrl = environment.config.property("app.authorizeUrl").getString(),
        accessTokenUrl = environment.config.property("app.accessTokenUrl").getString(),
        keySetUrl = environment.config.property("app.keySetUrl").getString(),
        issuer = environment.config.property("app.issuer").getString(),
        esiDomain = environment.config.property("app.esiDomain").getString(),
        esiDatasource = environment.config.property("app.esiDatasource").getString(),
        secure = environment.config.property("app.secure").getString(),
        cors = environment.config.property("app.corsDomain").getString(),
        alliances = environment.config.property("app.allianceAllowlist").getString(),
    )

    // Remove all those DEBUG messages from the console
    (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("org.mongodb.driver").level = Level.ERROR

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
        val storage = directorySessionStorage(
            File(System.getProperty("java.io.tmpdir") + "/.eve-route-sessions"),
            cached = true
        )
        cookie<Session>("EVE_ROUTE_SESSION", storage) {
            cookie.extensions["SameSite"] = "lax"
            if (config.secure == "1") {
                cookie.extensions["secure"] = "true"
            }
            cookie.path = "/"
            serializer = GsonSessionSerializer(Session::class.java)
        }
    }

    install(Locations)

    install(Authentication) {
        val scopes = mapOf(
            "100" to listOf("esi-ui.write_waypoint.v1"),
            "010" to listOf("esi-location.read_location.v1"),
            "001" to listOf("esi-search.search_structures.v1", "esi-universe.read_structures.v1"),
            "110" to listOf(
                "esi-ui.write_waypoint.v1",
                "esi-location.read_location.v1"
            ),
            "101" to listOf(
                "esi-ui.write_waypoint.v1",
                "esi-search.search_structures.v1", "esi-universe.read_structures.v1"
            ),
            "011" to listOf(
                "esi-location.read_location.v1",
                "esi-search.search_structures.v1", "esi-universe.read_structures.v1"
            ),
            "111" to listOf(
                "esi-ui.write_waypoint.v1",
                "esi-location.read_location.v1",
                "esi-search.search_structures.v1", "esi-universe.read_structures.v1",
            ),
        )
        oauth("eve-oauth") {
            client = httpClient
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "eve",
                    authorizeUrl = config.authorizeUrl,
                    accessTokenUrl = config.accessTokenUrl,
                    requestMethod = HttpMethod.Post,
                    clientId = config.clientId,
                    clientSecret = config.clientSecret,
                    defaultScopes = scopes[application.locations.resolve<Login>(Login::class, this).features] ?:
                                    listOf()
                )
            }
            urlProvider = {
                config.callback
            }
        }
    }

    if (config.cors.isNotEmpty()) {
        install(CORS) {
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
            method(HttpMethod.Options)
            host(config.cors, listOf("http", "https"))
            allowCredentials = true
            allowNonSimpleContentTypes = true
            header(config.csrfHeaderKey)
        }
    }

    install(Routing) {
        frontend()
        authentication(config)
        gates(config)
        route(config)
        systems()
        connection(config)
    }
}
