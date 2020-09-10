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
    val cors: String
)

data class Session(
    val esiToken: EsiToken.Data? = null,
    val eveCharacter: EveCharacter? = null,
)

data class EveCharacter(
    val id: Int,
    val name: String,
    val allianceId: Int,
    val allianceName: String,
    val allianceTicker: String,
)
