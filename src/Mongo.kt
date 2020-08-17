package net.tkhamez.everoute

import net.tkhamez.everoute.data.Gate
import org.litote.kmongo.*

class Mongo(uri: String) {
    private val dbName = uri.substring(uri.lastIndexOf('/') + 1)
    private val client = KMongo.createClient(uri)
    private val database = client.getDatabase(dbName)

    fun getGates(): List<Gate> {
        val gates = mutableListOf<Gate>()
        database.getCollection<Gate>().find().forEach { gates.add(it) }
        return gates
    }

    fun storeGate(gate: Gate) {
        val col = database.getCollection<Gate>()
        val existingGate = col.findOne(Gate::id eq gate.id)
        if (existingGate == null) {
            col.insertOne(gate)
        } else {
            col.replaceOne(Gate::id eq gate.id, gate)
        }
    }

    fun removeOtherGates(gates: List<Gate>) {
        val col = database.getCollection<Gate>()

        val ids: MutableList<Long> = mutableListOf()
        gates.forEach { ids.add(it.id) }

        val gatesToDelete = col.find(Gate::id nin ids)
        gatesToDelete.forEach {
            col.deleteOne(Gate::id eq it.id)
        }
    }
}
