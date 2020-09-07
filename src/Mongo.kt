package net.tkhamez.everoute

import net.tkhamez.everoute.data.Alliance
import net.tkhamez.everoute.data.Ansiblex
import net.tkhamez.everoute.data.TemporaryConnection
import org.litote.kmongo.*
import java.util.*

class Mongo(uri: String) {
    private val dbName = uri.substring(uri.lastIndexOf('/') + 1)
    private val client = KMongo.createClient(uri)
    private val database = client.getDatabase(dbName)

    fun gatesGet(allianceId: Int): List<Ansiblex> {
        val gates = mutableListOf<Ansiblex>()
        database.getCollection<Ansiblex>("ansiblex-$allianceId").find().forEach { gates.add(it) }
        return gates
    }

    fun gateStore(ansiblex: Ansiblex, allianceId: Int) {
        val col = database.getCollection<Ansiblex>("ansiblex-$allianceId")
        val existingGate = col.findOne(Ansiblex::id eq ansiblex.id)
        if (existingGate == null) {
            col.insertOne(ansiblex)
        } else {
            col.replaceOne(Ansiblex::id eq ansiblex.id, ansiblex)
        }
    }

    fun gatesRemoveOther(ansiblexes: List<Ansiblex>, allianceId: Int) {
        val col = database.getCollection<Ansiblex>("ansiblex-$allianceId")

        val ids: MutableList<Long> = mutableListOf()
        ansiblexes.forEach { ids.add(it.id) }

        val gatesToDelete = col.find(Ansiblex::id nin ids)
        gatesToDelete.forEach {
            col.deleteOne(Ansiblex::id eq it.id)
        }
    }

    fun temporaryConnectionsGet(characterId: Int): List<TemporaryConnection> {
        val col = database.getCollection<TemporaryConnection>("temporary-connection")

        val existingConnections = mutableListOf<TemporaryConnection>()
        col.find(TemporaryConnection::characterId eq characterId).forEach {
            existingConnections.add(it)
        }

        return existingConnections
    }

    fun temporaryConnectionStore(tempConnection: TemporaryConnection) {
        val col = database.getCollection<TemporaryConnection>("temporary-connection")

        val existingConnection = col.findOne(
            TemporaryConnection::system1Id eq tempConnection.system1Id,
            TemporaryConnection::system2Id eq tempConnection.system2Id
        )

        if (existingConnection == null) {
            col.insertOne(tempConnection)
        }
    }

    fun temporaryConnectionsDeleteAllExpired() {
        val col = database.getCollection<TemporaryConnection>("temporary-connection")

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        col.deleteMany(TemporaryConnection::created lt calendar.time)
    }

    fun temporaryConnectionDelete(system1Id: Int, system2Id: Int) {
        val col = database.getCollection<TemporaryConnection>("temporary-connection")
        col.deleteOne(
            TemporaryConnection::system1Id eq system1Id,
            TemporaryConnection::system2Id eq system2Id
        )
    }

    fun allianceGet(allianceId: Int): Alliance? {
        val col = database.getCollection<Alliance>()
        return col.findOne(Alliance::id eq allianceId)
    }

    fun allianceUpdate(alliance: Alliance) {
        val col = database.getCollection<Alliance>()
        val existingAlliance = col.findOne(Alliance::id eq alliance.id)
        if (existingAlliance == null) {
            col.insertOne(alliance)
        } else {
            col.replaceOne(Alliance::id eq alliance.id, alliance)
        }
    }
}
