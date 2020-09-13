package net.tkhamez.everoute

import net.tkhamez.everoute.data.MongoAlliance
import net.tkhamez.everoute.data.MongoAnsiblex
import net.tkhamez.everoute.data.MongoTemporaryConnection
import org.litote.kmongo.*
import java.util.*

class Mongo(uri: String) {
    private val dbName = uri.substring(uri.lastIndexOf('/') + 1)
    private val client = KMongo.createClient(uri)
    private val database = client.getDatabase(dbName)

    fun gatesGet(allianceId: Int): List<MongoAnsiblex> {
        val gates = mutableListOf<MongoAnsiblex>()
        database.getCollection<MongoAnsiblex>("ansiblex-$allianceId").find().forEach { gates.add(it) }
        return gates
    }

    fun gateStore(ansiblex: MongoAnsiblex, allianceId: Int) {
        val col = database.getCollection<MongoAnsiblex>("ansiblex-$allianceId")
        val existingGate = col.findOne(MongoAnsiblex::id eq ansiblex.id)
        if (existingGate == null) {
            col.insertOne(ansiblex)
        } else {
            col.replaceOne(MongoAnsiblex::id eq ansiblex.id, ansiblex)
        }
    }

    fun gatesRemoveOther(ansiblexes: List<MongoAnsiblex>, allianceId: Int) {
        val col = database.getCollection<MongoAnsiblex>("ansiblex-$allianceId")

        val ids: MutableList<Long> = mutableListOf()
        ansiblexes.forEach { ids.add(it.id) }

        val gatesToDelete = col.find(MongoAnsiblex::id nin ids)
        gatesToDelete.forEach {
            col.deleteOne(MongoAnsiblex::id eq it.id)
        }
    }

    fun temporaryConnectionsGet(characterId: Int): List<MongoTemporaryConnection> {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")

        val existingConnections = mutableListOf<MongoTemporaryConnection>()
        col.find(MongoTemporaryConnection::characterId eq characterId).forEach {
            existingConnections.add(it)
        }

        return existingConnections
    }

    fun temporaryConnectionStore(tempConnection: MongoTemporaryConnection) {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")

        val existingConnection = col.findOne(
            MongoTemporaryConnection::system1Id eq tempConnection.system1Id,
            MongoTemporaryConnection::system2Id eq tempConnection.system2Id
        )

        if (existingConnection == null) {
            col.insertOne(tempConnection)
        }
    }

    fun temporaryConnectionsDeleteAllExpired() {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        col.deleteMany(MongoTemporaryConnection::created lt calendar.time)
    }

    fun temporaryConnectionDelete(system1Id: Int, system2Id: Int) {
        val col = database.getCollection<MongoTemporaryConnection>("temporary-connection")
        col.deleteOne(
            MongoTemporaryConnection::system1Id eq system1Id,
            MongoTemporaryConnection::system2Id eq system2Id
        )
    }

    fun allianceGet(allianceId: Int): MongoAlliance? {
        val col = database.getCollection<MongoAlliance>("alliance")
        return col.findOne(MongoAlliance::id eq allianceId)
    }

    fun allianceUpdate(alliance: MongoAlliance) {
        val col = database.getCollection<MongoAlliance>("alliance")
        val existingAlliance = col.findOne(MongoAlliance::id eq alliance.id)
        if (existingAlliance == null) {
            col.insertOne(alliance)
        } else {
            col.replaceOne(MongoAlliance::id eq alliance.id, alliance)
        }
    }
}
