/**
 * MongoDb documents
 */

package net.tkhamez.everoute.data

import java.util.Date

data class MongoAnsiblex(
    val id: Long, // unique together with allianceId
    val allianceId: Int,
    val name: String,
    val solarSystemId: Int,
)

data class MongoTemporaryConnection(
    val system1Id: Int, // unique together with system2Id and characterId
    val system2Id: Int,
    val characterId: Int,
    val system1Name: String,
    val system2Name: String,
    val created: Date,
)

data class MongoAlliance(
    val id: Int,
    val updated: Date,
)
