package net.tkhamez.everoute

import net.tkhamez.everoute.data.Ansiblex
import org.litote.kmongo.*

class Mongo(uri: String) {
    private val dbName = uri.substring(uri.lastIndexOf('/') + 1)
    private val client = KMongo.createClient(uri)
    private val database = client.getDatabase(dbName)

    fun getGates(allianceId: Int): List<Ansiblex> {
        val gates = mutableListOf<Ansiblex>()
        database.getCollection<Ansiblex>("ansiblex-$allianceId").find().forEach { gates.add(it) }
        return gates
    }

    fun storeGate(ansiblex: Ansiblex, allianceId: Int) {
        val col = database.getCollection<Ansiblex>("ansiblex-$allianceId")
        val existingGate = col.findOne(Ansiblex::id eq ansiblex.id)
        if (existingGate == null) {
            col.insertOne(ansiblex)
        } else {
            col.replaceOne(Ansiblex::id eq ansiblex.id, ansiblex)
        }
    }

    fun removeOtherGates(ansiblexes: List<Ansiblex>, allianceId: Int) {
        val col = database.getCollection<Ansiblex>("ansiblex-$allianceId")

        val ids: MutableList<Long> = mutableListOf()
        ansiblexes.forEach { ids.add(it.id) }

        val gatesToDelete = col.find(Ansiblex::id nin ids)
        gatesToDelete.forEach {
            col.deleteOne(Ansiblex::id eq it.id)
        }
    }
}
