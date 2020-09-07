/**
 * MongoDb documents
 */

package net.tkhamez.everoute.data

import java.util.Date

data class Ansiblex(
    val id: Long,
    val name: String,
    val solarSystemId: Int
)

data class TemporaryConnection(
    val system1Id: Int,
    val system1Name: String,
    val system2Id: Int,
    val system2Name: String,
    val characterId: Int,
    val created: Date
)

data class Alliance(
    val id: Int,
    val updated: Date
)
