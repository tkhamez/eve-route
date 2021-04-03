package net.tkhamez.everoute.database

import com.mongodb.client.MongoCollection
import net.tkhamez.everoute.data.*
import org.litote.kmongo.*
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

private const val COLLECTION_ANSIBLEX = "ansiblex"
private const val COLLECTION_TEMPORARY_CONNECTION = "temporary-connection"
private const val COLLECTION_ALLIANCE = "alliance"
private const val COLLECTION_LOGIN = "login"

class Mongo(uri: String): DbInterface {
    private val dbName = uri.substring(uri.lastIndexOf('/') + 1)
    private val client = KMongo.createClient(uri)
    private val database = client.getDatabase(dbName)

    override fun migrate() {
        migrate030()
        migrate070()
    }

    override fun gatesGet(allianceId: Int): List<MongoAnsiblex> {
        val gates = mutableListOf<MongoAnsiblex>()
        database.getCollection<MongoAnsiblex>(COLLECTION_ANSIBLEX)
            .find(MongoAnsiblex::alliances contains allianceId)
            .forEach { gates.add(it) }
        return gates
    }

    override fun gateStore(ansiblex: MongoAnsiblex, allianceId: Int) {
        val col = database.getCollection<MongoAnsiblex>(COLLECTION_ANSIBLEX)
        val existingAnsiblex: MongoAnsiblex? = col.findOne(MongoAnsiblex::id eq ansiblex.id)
        if (existingAnsiblex == null) {
            ansiblex.alliances.add(allianceId)
            col.insertOne(ansiblex)
        } else {
            if (!existingAnsiblex.alliances.contains(allianceId)) {
                existingAnsiblex.alliances.add(allianceId)
            }
            existingAnsiblex.name = ansiblex.name
            existingAnsiblex.regionId = ansiblex.regionId
            existingAnsiblex.source = ansiblex.source
            col.replaceOne(MongoAnsiblex::id eq ansiblex.id, existingAnsiblex)
        }
    }

    override fun gateDelete(ansiblexId: Long, allianceId: Int): Boolean {
        val col = database.getCollection<MongoAnsiblex>(COLLECTION_ANSIBLEX)
        val ansiblex: MongoAnsiblex? = col.findOne(MongoAnsiblex::id eq ansiblexId)
        if (ansiblex != null) {
            ansiblex.alliances.remove(allianceId)
            replaceOrDelete(col, ansiblex)
        }
        return true
    }

    override fun gatesRemoveSourceESI(allianceId: Int) {
        val col = database.getCollection<MongoAnsiblex>(COLLECTION_ANSIBLEX)
        col.find(or(
            MongoAnsiblex::source eq MongoAnsiblex.Source.ESI,
            MongoAnsiblex::source exists false
        )).forEach {
            it.alliances.remove(allianceId)
            replaceOrDelete(col, it)
        }
    }

    override fun gatesRemoveRegion(regionId: Int, allianceId: Int) {
        val col = database.getCollection<MongoAnsiblex>(COLLECTION_ANSIBLEX)
        col.find(MongoAnsiblex::regionId eq regionId).forEach {
            it.alliances.remove(allianceId)
            replaceOrDelete(col, it)
        }
    }

    override fun temporaryConnectionsGet(characterId: Int): List<MongoTemporaryConnection> {
        val col = database.getCollection<MongoTemporaryConnection>(COLLECTION_TEMPORARY_CONNECTION)

        val existingConnections = mutableListOf<MongoTemporaryConnection>()
        col.find(MongoTemporaryConnection::characterId eq characterId).forEach {
            existingConnections.add(it)
        }

        return existingConnections
    }

    override fun temporaryConnectionStore(tempConnection: MongoTemporaryConnection) {
        val col = database.getCollection<MongoTemporaryConnection>(COLLECTION_TEMPORARY_CONNECTION)
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
        val col = database.getCollection<MongoTemporaryConnection>(COLLECTION_TEMPORARY_CONNECTION)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        col.deleteMany(MongoTemporaryConnection::created lt calendar.time)
    }

    override fun temporaryConnectionDelete(system1Id: Int, system2Id: Int, characterId: Int) {
        val col = database.getCollection<MongoTemporaryConnection>(COLLECTION_TEMPORARY_CONNECTION)
        col.deleteOne(
            MongoTemporaryConnection::system1Id eq system1Id,
            MongoTemporaryConnection::system2Id eq system2Id,
            MongoTemporaryConnection::characterId eq characterId,
        )
    }

    override fun allianceAdd(allianceId: Int) {
        val col = database.getCollection<MongoAlliance>(COLLECTION_ALLIANCE)
        val existingAlliance = col.findOne(MongoAlliance::id eq allianceId)
        if (existingAlliance == null) {
            col.insertOne(MongoAlliance(id = allianceId, updated = Date(0)))
        }
    }

    override fun allianceGet(allianceId: Int): MongoAlliance? {
        val col = database.getCollection<MongoAlliance>(COLLECTION_ALLIANCE)
        return col.findOne(MongoAlliance::id eq allianceId)
    }

    override fun allianceUpdate(alliance: MongoAlliance) {
        val col = database.getCollection<MongoAlliance>(COLLECTION_ALLIANCE)
        val existingAlliance = col.findOne(MongoAlliance::id eq alliance.id)
        if (existingAlliance == null) {
            col.insertOne(alliance)
        } else {
            col.replaceOne(MongoAlliance::id eq alliance.id, alliance)
        }
    }

    override fun loginRegister(characterId: Int) {
        val col = database.getCollection<MongoLogin>(COLLECTION_LOGIN)
        val today = LocalDate.now(ZoneOffset.UTC)
        val filter = and(
            MongoLogin::characterId eq characterId,
            MongoLogin::year eq today.year,
            MongoLogin::month eq today.monthValue,
        )
        val existingLogin = col.findOne(filter)
        if (existingLogin == null) {
            col.insertOne(MongoLogin(
                characterId = characterId,
                year = today.year,
                month = today.monthValue,
                count = 1,
            ))
        } else {
            existingLogin.count ++
            col.replaceOne(filter, existingLogin)
        }
    }

    private fun replaceOrDelete(col: MongoCollection<MongoAnsiblex>, ansiblex: MongoAnsiblex) {
        if (ansiblex.alliances.size > 0) {
            col.replaceOne(MongoAnsiblex::id eq ansiblex.id, ansiblex)
        } else {
            col.deleteOne(MongoAnsiblex::id eq ansiblex.id)
        }
    }

    private fun migrate030() {
        // 0.2.0 -> 0.3.0: copy "allianceId" to "alliances"
        val col = database.getCollection<MongoAnsiblex>(COLLECTION_ANSIBLEX)
        database.getCollection<MongoAnsiblexV02>(COLLECTION_ANSIBLEX)
            .find(MongoAnsiblexV02::allianceId gt 0)
            .forEach {
                val filter = and(MongoAnsiblex::id eq it.id, MongoAnsiblex::alliances exists true)
                var gateV03 = col.findOne(filter)
                if (gateV03 == null) {
                    gateV03 = MongoAnsiblex(
                        id = it.id,
                        name = it.name,
                        solarSystemId = it.solarSystemId,
                    )
                    gateV03.alliances.add(it.allianceId)
                    col.insertOne(gateV03)
                } else {
                    gateV03.alliances.add(it.allianceId)
                    col.replaceOne(filter, gateV03)
                }
            }
        col.deleteMany(MongoAnsiblex::alliances exists false)
    }

    private fun migrate070() {
        // 0.6.0 -> 0.7.0 delete gates without region
        val col = database.getCollection<MongoAnsiblex>(COLLECTION_ANSIBLEX)
        col.deleteMany(MongoAnsiblex::regionId exists false)
        col.deleteMany(MongoAnsiblex::regionId eq null)
    }
}
