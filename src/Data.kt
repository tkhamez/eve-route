package net.tkhamez.everoute

data class Config(
        val db: String,
        val clientId: String,
        val clientSecret: String,
        val callback: String,
        val authorizeUrl: String,
        val accessTokenUrl: String
)

data class Session(
        val authToken: AuthToken? = null,
        val esiVerify: EsiVerify? = null
)

data class AuthToken(
        val refreshToken: String,
        var accessToken: String,
        var expiresOn: String
)

data class EsiVerify(
        val CharacterID: Long,
        val CharacterName: String,
        val ExpiresOn: String, // only used during login, never updated!
        val Scopes: String
)

data class EsiRefreshToken(
        val access_token: String,
        val expires_in: Int
)

data class EsiSearchStructure(val structure: List<Long> = emptyList())

data class EsiStructure(
        val name: String,
        // owner_id
        // position
        val solar_system_id: Int,
        val type_id: Int
)

data class ResponseUser(
        val id: Long,
        val name: String
)

data class ResponseFindGates(
        var success: Boolean = false,
        var message: String = "",
        var gates: MutableList<EsiStructure> = mutableListOf()
)
