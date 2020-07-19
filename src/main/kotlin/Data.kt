package net.tkhamez.everoute

data class Session(val eveAuth: MutableMap<String, Any?> = mutableMapOf())

data class EsiEveAuth(
        val CharacterID: Long? = null,
        val CharacterName: String? = null,
        val ExpiresOn: String? = null,
        val Scopes: String? = null
)

data class EsiRefreshToken(
        val access_token: String,
        val expires_in: Int,
        var expiresOn: String? = null
)

data class EsiSearchStructure(val structure: List<Long> = emptyList())

data class EsiStructure(
        val name: String = "",
        // owner_id
        // position
        val solar_system_id: Int? = null,
        val type_id: Int? = null
)

data class ResponseFindGates(
        var success: Boolean = false,
        var message: String = "",
        var gates: MutableList<EsiStructure> = mutableListOf()
)
