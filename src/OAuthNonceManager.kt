package net.tkhamez.everoute

import io.ktor.application.*
import io.ktor.sessions.*
import io.ktor.util.*
import net.tkhamez.everoute.data.Session

@KtorExperimentalAPI
class OAuthNonceManager(private val call: ApplicationCall) : NonceManager {
    override suspend fun newNonce(): String {
        val nonce = generateNonce()
        val session = call.sessions.get<Session>() ?: Session()
        call.sessions.set(session.copy(oAuthNonce = nonce))
        return nonce
    }

    override suspend fun verifyNonce(nonce: String): Boolean {
        if (nonce != call.sessions.get<Session>()?.oAuthNonce) {
            throw IllegalArgumentException("Invalid nonce")
        }
        return true
    }
}
