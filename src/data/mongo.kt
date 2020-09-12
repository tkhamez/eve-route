/**
 * MongoDb documents
 */

package net.tkhamez.everoute.data

import java.util.Date

data class MongoAnsiblex(
    val id: Long,
    val name: String,
    val solarSystemId: Int
)

data class MongoTemporaryConnection(
    val system1Id: Int,
    val system1Name: String,
    val system2Id: Int,
    val system2Name: String,
    val characterId: Int,
    val created: Date
)

data class MongoAlliance(
    val id: Int,
    val updated: Date
)
