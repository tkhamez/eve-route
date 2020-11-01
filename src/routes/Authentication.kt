package net.tkhamez.everoute.routes

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import net.tkhamez.everoute.db
import net.tkhamez.everoute.EsiToken
import net.tkhamez.everoute.HttpRequest
import net.tkhamez.everoute.Login
import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.gson
import org.slf4j.Logger
import java.net.URL
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@KtorExperimentalLocationsAPI
fun Route.authentication(config: Config) {

    /**
     * Intercept all non-public routes below /api/ and return 403 if client is not logged in.
     *
     * Check CSRF token of all POST, PUT and DELETE request.
     */
    intercept(ApplicationCallPipeline.Features) {
        val session = call.sessions.get<Session>() ?: Session()
        if (session.started == null) { // first request
            call.sessions.set(session.copy(started = Date().time))
        }

        // check login
        val path = call.request.path()
        val publicRoutes = listOf(
            "/api/auth/login",
            "/api/auth/login/000",
            "/api/auth/login/100",
            "/api/auth/login/010",
            "/api/auth/login/001",
            "/api/auth/login/110",
            "/api/auth/login/101",
            "/api/auth/login/011",
            "/api/auth/login/111",
            "/api/auth/result",
        )
        if (path.indexOf("/api/") == 0 && publicRoutes.indexOf(path) == -1 && session.eveCharacter == null) {
            call.respond(HttpStatusCode.Unauthorized, "")
            return@intercept finish()
        }

        // check CSRF token
        if (
            call.request.httpMethod in arrayOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete) &&
            (session.csrfToken == null || session.csrfToken != call.request.header(config.csrfHeaderKey))
        ) {
            call.respond(HttpStatusCode.Forbidden, "")
            return@intercept finish()
        }
    }

    authenticate("eve-oauth") {
        // URL before redirect to EVE
        location<Login> {
            handle {}
        }

        // Callback URL
        route("/api/auth/login") {
            handle {
                val session = call.sessions.get<Session>() ?: Session()
                val log = call.application.environment.log
                val httpRequest = HttpRequest(config, log)

                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                if (principal == null) {
                    call.sessions.set(session.copy(loginResult = ResponseCodes.LoginSsoFailed))
                    call.respondRedirect("/")
                    return@handle
                }

                val tokenVerify = verify(principal.accessToken, config, log)
                if (tokenVerify == null) {
                    call.sessions.set(session.copy(loginResult = ResponseCodes.LoginEsiErrorVerify))
                    call.respondRedirect("/")
                    return@handle
                }

                val character = getCharacter(tokenVerify, config, httpRequest)
                if (character == null) {
                    call.sessions.set(session.copy(loginResult = getCharacterResult))
                } else {
                    call.sessions.set(
                        session.copy(
                            esiToken = EsiToken.Data(
                                accessToken = principal.accessToken,
                                refreshToken = principal.refreshToken.toString(),
                                expiresOn = tokenVerify.exp.toLong(),
                            ),
                            eveCharacter = character,
                            csrfToken = randomString(),
                        )
                    )
                    db(config.db).loginRegister(character.id)
                }

                call.respondRedirect("/")
            }
        }
    }

    get("/api/auth/result") {
        val session = call.sessions.get<Session>()
        val response = ResponseMessage(code = session?.loginResult)
        if (session != null) {
            call.sessions.set(session.copy(loginResult = null))
        }
        call.respondText(gson.toJson(response), contentType = ContentType.Application.Json)
    }

    get("/api/auth/user") {
        val session = call.sessions.get<Session>()
        var data: ResponseAuthUser? = null
        if (session?.eveCharacter != null && session.csrfToken != null) {
            data = ResponseAuthUser(
                name = session.eveCharacter.name,
                allianceName = session.eveCharacter.allianceName,
                allianceTicker = session.eveCharacter.allianceTicker,
                roles = session.eveCharacter.roles,
                csrfHeaderKey = config.csrfHeaderKey,
                csrfToken = session.csrfToken,
            )
        }
        call.respondText(gson.toJson(data), contentType = ContentType.Application.Json)
    }

    get("/api/auth/logout") {
        call.sessions.set(Session())
        call.respondText("")
    }
}

private fun verify(token: String?, config: Config, log: Logger): EsiTokenVerify? {
    val jwt = JWT.decode(token)
    val jwkProvider = UrlJwkProvider(URL(config.keySetUrl))
    val jwk = jwkProvider.get(jwt.keyId)

    val algorithm = when (jwk.algorithm) {
        "RS256" -> {
            val publicKey = jwk.publicKey as? RSAPublicKey
            Algorithm.RSA256(publicKey, null)
        }
        "ES256" -> {
            val publicKey = jwk.publicKey as? ECPublicKey
            Algorithm.ECDSA256(publicKey, null)
        }
        else -> null
    } ?: return null

    val verifier = JWT.require(algorithm) // signature
        .withIssuer(config.issuer) // iss
        .build()
    val decoded: DecodedJWT
    try {
        decoded = verifier.verify(token)
    } catch (e: Exception) {
        log.info(e.message)
        return null
    }

    val json = String(Base64.getDecoder().decode(decoded.payload))
    return gson.fromJson(json, EsiTokenVerify::class.java)
}

private var getCharacterResult: ResponseCodes? = null

private suspend fun getCharacter(tokenVerify: EsiTokenVerify, config: Config, httpRequest: HttpRequest): EveCharacter? {
    var eveCharacter: EveCharacter? = null
    val characterId = tokenVerify.sub.replace("CHARACTER:EVE:", "").toInt()

    val esiAffiliation = httpRequest.post<Array<EsiAffiliation>>(
        "latest/characters/affiliation/?datasource=${config.esiDatasource}",
        "[$characterId]"
    )
    val allianceId = esiAffiliation?.get(0)?.alliance_id

    if (allianceId == null) {
        getCharacterResult = ResponseCodes.LoginEsiErrorAlliance
    } else if (config.alliances.isNotEmpty() && !config.alliances.contains(allianceId)) {
        getCharacterResult = ResponseCodes.LoginWrongAlliance
    } else {
        val alliance = httpRequest.get<EsiAlliance>(
            "latest/alliances/$allianceId/?datasource=${config.esiDatasource}",
            "[$characterId]"
        )
        eveCharacter = EveCharacter(
            id = characterId,
            name = tokenVerify.name,
            allianceId = allianceId,
            allianceName = alliance?.name ?: "",
            allianceTicker = alliance?.ticker ?: "",
            roles = if (config.roleImport.contains(characterId)) listOf("import") else listOf(),
        )
    }

    return eveCharacter
}

private fun randomString(): String {
    val random = SecureRandom()
    val bytes = ByteArray(47)
    random.nextBytes(bytes)

    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
