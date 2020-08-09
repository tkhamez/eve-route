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
        val esiDomain: String
)

data class Session(
        val esiToken: EsiToken.Data? = null,
        val esiVerify: EsiVerify? = null
)
