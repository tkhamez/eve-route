package net.tkhamez.everoute

data class Session(val eveAuth: Map<String, Any?> = emptyMap())

data class EveAuth(
        val CharacterID: Long? = null,
        val CharacterName: String? = null,
        val ExpiresOn: String? = null,
        val Scopes: String? = null
)
