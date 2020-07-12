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
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
//import kotlinx.coroutines.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.UserAgent
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
    install(Sessions) {
        cookie<Session>(
                "SESSION",
                directorySessionStorage(File(".sessions"), cached = true)
        ) {
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
            val session = call.sessions.get<Session>()
            if (session?.eveAuth != null && session.eveAuth.containsKey("id")) {
                //println(session.eveAuth)
                call.respondText(
                        "HI ${session.eveAuth["name"]}, <a href='/logout'>logout</a>",
                        contentType = ContentType.Text.Html
                )
            } else {
                call.respondText("<a href='/login'>login</a>", contentType = ContentType.Text.Html)
            }
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

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
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
