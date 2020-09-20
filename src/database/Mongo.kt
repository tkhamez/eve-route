package net.tkhamez.everoute.database

import net.tkhamez.everoute.data.MongoAlliance
import net.tkhamez.everoute.data.MongoAnsiblex
import net.tkhamez.everoute.data.MongoTemporaryConnection
import org.litote.kmongo.*
import java.util.*

class Mongo(uri: String): DbInterface {
    private val dbName = uri.substring(uri.lastIndexOf('/') + 1)
    private val client = KMongo.createClient(uri)
    private val database = client.getDatabase(dbName)

    override fun gatesGet(allianceId: Int): List<MongoAnsiblex> {
        val gates = mutableListOf<MongoAnsiblex>()
        database.getCollection<MongoAnsiblex>("ansiblex")
            .find(MongoAnsiblex::allianceId eq allianceId)
            .forEach { gates.add(it) }
        return gates
    }

    override fun gateStore(ansiblex: MongoAnsiblex, allianceId: Int) {
        val col = database.getCollection<MongoAnsiblex>("ansiblex")
        val filter = and(
            MongoAnsiblex::id eq ansiblex.id,
            MongoAnsiblex::allianceId eq allianceId
        )
        val existingGate = col.findOne(filter)
        if (existingGate == null) {
            col.insertOne(ansiblex)
        } else {
            col.replaceOne(filter, ansiblex)
        }
    }

    override fun gatesRemoveOther(ansiblexes: List<MongoAnsiblex>, allianceId: Int) {
        val col = database.getCollection<MongoAnsiblex>("ansiblex")

        val ids: MutableList<Long> = mutableListOf()
        ansiblexes.forEach { ids.add(it.id) }

        val gatesToDelete = col.find(MongoAnsiblex::id nin ids, MongoAnsiblex::allianceId eq allianceId)
        gatesToDelete.forEach {
            col.deleteOne(MongoAnsiblex::id eq it.id, MongoAnsiblex::allianceId eq allianceId)
        }
    }

    override fun temporaryConnectionsGet(characterId: Int): List<MongoTemporaryConnection> {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")

        val existingConnections = mutableListOf<MongoTemporaryConnection>()
        col.find(MongoTemporaryConnection::characterId eq characterId).forEach {
            existingConnections.add(it)
        }

        return existingConnections
    }

    override fun temporaryConnectionStore(tempConnection: MongoTemporaryConnection) {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")
        val filter = and(
            MongoTemporaryConnection::system1Id eq tempConnection.system1Id,
            MongoTemporaryConnection::system2Id eq tempConnection.system2Id,
            MongoTemporaryConnection::characterId eq tempConnection.characterId,
        )
        val existingConnection = col.findOne(filter)
        if (existingConnection == null) {
            col.insertOne(tempConnection)
        } else {
            col.replaceOne(filter, tempConnection)
        }
    }

    override fun temporaryConnectionsDeleteAllExpired() {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        col.deleteMany(MongoTemporaryConnection::created lt calendar.time)
    }

    override fun temporaryConnectionDelete(system1Id: Int, system2Id: Int, characterId: Int) {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")
        col.deleteOne(
            MongoTemporaryConnection::system1Id eq system1Id,
            MongoTemporaryConnection::system2Id eq system2Id,
            MongoTemporaryConnection::characterId eq characterId,
        )
    }

    override fun allianceGet(allianceId: Int): MongoAlliance? {
        val col = database.getCollection<MongoAlliance>("alliance")
        return col.findOne(MongoAlliance::id eq allianceId)
    }

    override fun allianceUpdate(alliance: MongoAlliance) {
        val col = database.getCollection<MongoAlliance>("alliance")
        val existingAlliance = col.findOne(MongoAlliance::id eq alliance.id)
        if (existingAlliance == null) {
            col.insertOne(alliance)
        } else {
            col.replaceOne(MongoAlliance::id eq alliance.id, alliance)
        }
    }
}
