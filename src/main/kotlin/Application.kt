package net.tkhamez.everoute

import com.google.gson.Gson
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
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
//import kotlinx.coroutines.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.UserAgent
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
        exception<Throwable> {
            call.respondText("Error.", status = HttpStatusCode.InternalServerError)
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

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        install(UserAgent) {
            agent = "eve-route/0.0.1 Ktor http-client"
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
                        defaultScopes = listOf("esi-location.read_location.v1", "esi-ui.write_waypoint.v1")
                )
            }
            urlProvider = {
                @Suppress("EXPERIMENTAL_API_USAGE")
                environment.config.property("eve.callback").getString()
            }
        }
    }

    routing {
        get("/") {
            val resource = Thread.currentThread().contextClassLoader.getResource("public/index.html")
            if (resource != null) {
                call.respondText(File(resource.file).readText(), contentType = ContentType.Text.Html)
            } else {
                call.respondText("File not found.")
            }
        }

        get("/api/user") {
            val session = call.sessions.get<Session>()
            var data = emptyMap<String, Any?>()
            if (session?.eveAuth != null && session.eveAuth.containsKey("id")) {
                data = mapOf(
                    "id" to session.eveAuth["id"],
                    "name" to session.eveAuth["name"]
                )
            }
            call.respondText(Gson().toJson(data), contentType = ContentType.Application.Json)
        }

        get("/logout") {
            val session = call.sessions.get<Session>() ?: Session()
            call.sessions.set(session.copy(eveAuth = emptyMap()))
            call.respondRedirect("/")
        }

        authenticate("eve-oauth") {
            route("/login") {
                handle {
                    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                    if (principal != null) {
                        val eveAuth = httpClient.get<EveAuth>("https://login.eveonline.com/oauth/verify") {
                            header("Authorization", "Bearer ${principal.accessToken}")
                        }
                        if (eveAuth.CharacterID != null) {
                            val session = call.sessions.get<Session>() ?: Session()
                            call.sessions.set(session.copy(eveAuth = mapOf(
                                    "id" to eveAuth.CharacterID,
                                    "name" to eveAuth.CharacterName,
                                    "accessToken" to principal.accessToken,
                                    "scopes" to eveAuth.Scopes,
                                    "expiresOn" to eveAuth.ExpiresOn,
                                    "refreshToken" to principal.refreshToken
                            )))
                        }
                    }
                    call.respondRedirect("/")
                }
            }
        }

        static("/") {
            resources("public")
        }
    }
}

data class Session(val eveAuth: Map<String, Any?> = emptyMap())

data class EveAuth(
        val CharacterID: Long? = null,
        val CharacterName: String? = null,
        val ExpiresOn: String? = null,
        val Scopes: String? = null
)
