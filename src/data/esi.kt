/**
 * ESI responses.
 */

package net.tkhamez.everoute.data

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
