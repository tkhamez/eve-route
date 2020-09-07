/**
 * Application API responses.
 *
 * These have JS counterparts located in frontend/src/data.js
 */

package net.tkhamez.everoute.data

import net.tkhamez.everoute.EveRoute
import java.util.Date

enum class ResponseCodes {
    AlreadyUpdated,
    AuthError,
    ConnectionStored,
    Error,
    FailedToStoreData,
    MissingInput,
    SearchError,
    SearchSuccess,
    Success,
    SystemNotFound,
    WrongSearchTerm
}

data class ResponseMessage(
    var code: ResponseCodes? = null,
    var success: Boolean? = null,
    var param: String? = null
)

data class ResponseAuthUser(
    val characterId: Int,
    val characterName: String,
    val allianceId: Int?
)

data class ResponseGates(
    var code: ResponseCodes? = null,
    var ansiblexes: MutableList<Ansiblex> = mutableListOf()
)

data class ResponseGatesUpdated(
    var code: ResponseCodes? = null,
    var allianceId: Int? = null,
    var updated: Date? = null
)

data class ResponseTemporaryConnections(
    var code: ResponseCodes? = null,
    var temporaryConnections: MutableList<TemporaryConnection> = mutableListOf()
)

data class ResponseSystems(
    var systems: MutableList<String> = mutableListOf()
)

data class ResponseRouteLocation(
    var code: ResponseCodes? = null,
    var solarSystemId: Int? = null,
    var solarSystemName: String? = null
)

data class ResponseRouteFind(
    var code: ResponseCodes? = null,
    var route: List<EveRoute.Waypoint> = listOf()
)
