/**
 * OAuth/ESI responses.
 */

package net.tkhamez.everoute.data

data class EsiVerify(
    val CharacterID: Int,
    val CharacterName: String,
    val ExpiresOn: String,
    val Scopes: String
)

data class TokenVerify(
    //val scp: List<String>,
    //val jti: String,
    //val kid: String,
    val sub: String,
    //val azp: String,
    val name: String,
    //val owner: String,
    val exp: String,
    //val iss: String,
)

data class EsiAffiliation(
    val alliance_id: Int,
    val character_id: Int,
    val corporation_id: Int,
    val faction_id: Int
)

data class EsiAlliance(
    val name: String,
    val ticker: String,
)

data class EsiRefreshToken(
    val access_token: String,
    val expires_in: Int
)

data class EsiLocation(
    var solar_system_id: Int
)

data class EsiSearchStructure(val structure: List<Long> = emptyList())

data class EsiStructure(
    val name: String,
    // owner_id
    // position
    val solar_system_id: Int,
    val type_id: Int
)
