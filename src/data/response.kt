/**
 * Application API responses.
 *
 * These have JS counterparts located in frontend/src/data.js
 */

package net.tkhamez.everoute.data

import net.tkhamez.everoute.EveRoute
import java.util.Date

enum class ResponseCodes {
    WrongSearchTerm,
    AuthAllianceOrTokenFail,
    AlreadyUpdated,
    SearchError,
    SearchSuccess
}

data class ResponseMessage(
    var code: ResponseCodes? = null,
    var param: String? = null
)

data class ResponseAuthUser(
    val characterId: Int,
    val characterName: String,
    val allianceId: Int?
)

data class ResponseGates(
    var message: String = "",
    var ansiblexes: MutableList<Ansiblex> = mutableListOf()
)

data class ResponseGatesUpdated(
    var message: String = "",
    var allianceId: Int? = null,
    var updated: Date? = null
)

data class ResponseSystems(
    var systems: MutableList<String> = mutableListOf()
)

data class ResponseRouteFind(
    var message: String = "",
    var route: List<EveRoute.Waypoint> = listOf()
)

data class ResponseRouteSet(
    var message: String = ""
)
