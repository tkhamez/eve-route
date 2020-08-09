/**
 * MongoDb documents
 */

package net.tkhamez.everoute.data

data class Gate(
    val id: Long,
    val name: String,
    val solarSystemId: Int
)
