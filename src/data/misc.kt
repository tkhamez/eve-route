/**
 * Miscellaneous data classes.
 */

package net.tkhamez.everoute.data

import net.tkhamez.everoute.EsiToken

data class Config(
    val db: String,
    val clientId: String,
    val clientSecret: String,
    val callback: String,
    val authorizeUrl: String,
    val accessTokenUrl: String,
    val verifyUrl: String,
    val esiDomain: String,
    val esiDatasource: String,
    val cors: String,
    val alliances: String,
)

data class Session(
    val loginResult: ResponseCodes? = null,
    val esiToken: EsiToken.Data? = null,
    val eveCharacter: EveCharacter? = null,
    val avoidedSystems: MutableSet<Int>? = null,
    val removedConnections: MutableSet<ConnectedSystems>? = null,
)

data class EveCharacter(
    val id: Int,
    val name: String,
    val allianceId: Int,
    val allianceName: String,
    val allianceTicker: String,
)

data class System(
    val id: Int,
    val name: String,
)

data class ConnectedSystems(
    val system1: String,
    val system2: String,
)
