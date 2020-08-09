/**
 * Application API responses.
 */

package net.tkhamez.everoute.data

data class ResponseUser(
    val id: Long,
    val name: String
)

data class ResponseFindGates(
    var success: Boolean = false,
    var message: String = "",
    var gates: MutableList<EsiStructure> = mutableListOf()
)
