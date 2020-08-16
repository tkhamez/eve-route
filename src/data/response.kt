/**
 * Application API responses.
 */

package net.tkhamez.everoute.data

data class ResponseUser(
    val id: Long,
    val name: String
)

data class ResponseGates(
    var success: Boolean = false,
    var message: String = "",
    var gates: MutableList<Gate> = mutableListOf()
)

data class ResponseCalculate(
    var route: List<System> = listOf()
)
