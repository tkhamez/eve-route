/**
 * MongoDb documents
 */

package net.tkhamez.everoute.data

import java.util.*

data class Ansiblex(
    val id: Long,
    val name: String,
    val solarSystemId: Int
)

data class Alliance(
    val id: Int,
    val updated: Date
)
