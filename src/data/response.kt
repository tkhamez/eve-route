/**
 * Application API responses.
 */

package net.tkhamez.everoute.data

import net.tkhamez.everoute.EveRoute

data class ResponseAuthUser(
    val id: Long,
    val name: String
)

data class ResponseGates(
    var success: Boolean = false,
    var message: String = "",
    var gates: MutableList<Gate> = mutableListOf()
)

data class ResponseRouteCalculate(
    var route: List<EveRoute.Waypoint> = listOf()
)

data class ResponseRouteSet(
    var message: String = ""
)

