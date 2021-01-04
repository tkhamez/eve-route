package net.tkhamez.everoute.database

import net.tkhamez.everoute.data.MongoAlliance
import net.tkhamez.everoute.data.MongoAnsiblex
import net.tkhamez.everoute.data.MongoTemporaryConnection
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

class Exposed(uri: String): DbInterface {
    private var url: String = ""
    private var user: String = ""
    private var password: String = ""

    object Ansiblex: IdTable<Long>() {
        override val id = long("id").entityId()
        val name = varchar("name", 255)
        val solarSystemId = integer("solarSystemId")
        val regionId = integer("regionId").nullable()
        val source1 = varchar("source", 255).nullable()
        override val primaryKey = PrimaryKey(id, name = "PK_Ansiblex")
    }

    object Alliance: IdTable<Int>() {
        override val id = integer("id").entityId()
        val updated = datetime("updated")
        override val primaryKey = PrimaryKey(id, name = "PK_Alliance")
    }

    object AnsiblexAlliance: Table() {
        val ansiblexId = reference("ansiblexId", Ansiblex)
        val allianceId = reference("allianceId", Alliance)
        override val primaryKey = PrimaryKey(ansiblexId, allianceId, name = "PK_AnsiblexAlliance")
    }

    object TemporaryConnection: IntIdTable() {
        val system1Id = integer("system1Id")
        val system2Id = integer("system2Id")
        val characterId = integer("characterId")
        val system1Name = varchar("system1Name", 255)
        val system2Name = varchar("system2Name", 255)
        val created = datetime("created")
        init {
            index(isUnique = true, system1Id, system2Id, characterId)
        }
    }

    object Login: IntIdTable() {
        val characterId = integer("characterId")
        val year = integer("year")
        val month = integer("month")
        val count = integer("count")
        init {
            index(isUnique = true, characterId, year, month)
        }
    }

    init {
        if (uri.startsWith("jdbc:postgresql:") || uri.startsWith("jdbc:mysql:") || uri.startsWith("jdbc:mariadb:")) {
            // e.g. jdbc:mysql://eve_route:password@localhost:3306/eve_route?serverTimezone=UTC
            val scheme = uri.substring(0, uri.indexOf("://") + 3)
            val hostAndRest = uri.substring(uri.indexOf("@") + 1)
            url = "$scheme$hostAndRest"
            val userAndPassword = uri.substring(uri.indexOf("://") + 3, uri.indexOf("@"))
            user = userAndPassword.substring(0, userAndPassword.indexOf(":"))
            password = userAndPassword.substring(userAndPassword.indexOf(":") + 1)
            user = URLDecoder.decode(user, StandardCharsets.UTF_8.name())
            password = URLDecoder.decode(password, StandardCharsets.UTF_8.name())
        } else if (uri.startsWith("jdbc:sqlite:") || uri.startsWith("jdbc:h2:")) {
            url = uri
        } else {
            throw Exception("Database not supported.")
        }

        Database.connect(url = url, user = user, password = password)
    }

    override fun migrate() {
        // change log level to "debug" in logback.xml to see the SQL statements
        //transaction { SchemaUtils.createMissingTablesAndColumns(
        //    Alliance, Ansiblex, AnsiblexAlliance, TemporaryConnection, Login
        //) }

        var vendor = url.substring(5, url.indexOf(":", 5))
        if (vendor == "mariadb") {
            vendor = "mysql"
        }
        val enc = if (vendor == "h2") "\"" else ""
        val flyway = Flyway
            .configure()
            .dataSource(url, user, password)
            .locations("db/migration/$vendor")
            .load()

        // Check if migration v1 from application v0.2.0 already exists
        var tablesAnsiblexExist = true
        var tablesFlywayExist = true
        transaction {
            try {
                TransactionManager.current().exec("SELECT 1 FROM Ansiblex")
            } catch (e: Exception) {
                tablesAnsiblexExist = false
            }
            try {
                TransactionManager.current().exec("SELECT 1 FROM ${enc}flyway_schema_history${enc}")
            } catch (e: Exception) {
                tablesFlywayExist = false
            }
        }
        if (tablesAnsiblexExist && ! tablesFlywayExist) {
            flyway.baseline()
        }

        flyway.migrate()
    }

    override fun gatesGet(allianceId: Int): List<MongoAnsiblex> {
        val ansiblexes = mutableListOf<MongoAnsiblex>()

        transaction {
            val ids = mutableListOf<Long>()
            AnsiblexAlliance
                .select { AnsiblexAlliance.allianceId eq allianceId }
                .forEach { ids.add(it[AnsiblexAlliance.ansiblexId].value) }

            Ansiblex.select { Ansiblex.id inList ids }.forEach {
                ansiblexes.add(
                    MongoAnsiblex(
                        id = it[Ansiblex.id].value,
                        name = it[Ansiblex.name],
                        solarSystemId = it[Ansiblex.solarSystemId],
                    )
                )
            }
        }

        return ansiblexes
    }

    override fun gateStore(ansiblex: MongoAnsiblex, allianceId: Int) {
        transaction {
            val existingAnsiblex = Ansiblex.select { Ansiblex.id eq ansiblex.id }.firstOrNull()
            if (existingAnsiblex == null) {
                val newAnsiblexId = Ansiblex.insertAndGetId {
                    it[id] = ansiblex.id
                    it[name] = ansiblex.name
                    it[solarSystemId] = ansiblex.solarSystemId
                    it[regionId] = ansiblex.regionId
                    it[source1] = ansiblex.source.toString()
                }
                AnsiblexAlliance.insert {
                    it[ansiblexId] = newAnsiblexId
                    it[AnsiblexAlliance.allianceId] = allianceId
                }
            } else {
                Ansiblex.update({ Ansiblex.id eq existingAnsiblex[Ansiblex.id] }) {
                    it[id] = ansiblex.id
                    it[name] = ansiblex.name
                    it[solarSystemId] = ansiblex.solarSystemId
                    it[regionId] = ansiblex.regionId
                    it[source1] = ansiblex.source.toString()
                }
                AnsiblexAlliance.insertIgnore {
                    it[ansiblexId] = existingAnsiblex[Ansiblex.id]
                    it[AnsiblexAlliance.allianceId] = allianceId
                }
            }
        }
    }

    override fun gatesRemoveSourceESI(allianceId: Int) {
        transaction {
            // Find all with source "ESI"
            val ids = mutableListOf<Long>()
            Ansiblex
                .select { Ansiblex.source1 eq MongoAnsiblex.Source.ESI.toString() or (Ansiblex.source1 eq null) }
                .forEach { ids.add(it[Ansiblex.id].value) }

            // Delete relations
            AnsiblexAlliance.deleteWhere {
                AnsiblexAlliance.allianceId eq allianceId and (AnsiblexAlliance.ansiblexId inList ids)
            }

            // Delete Ansiblex gates without a relation
            val withRelation = mutableSetOf<Long>()
            AnsiblexAlliance.selectAll().forEach { withRelation.add(it[AnsiblexAlliance.ansiblexId].value) }
            Ansiblex.deleteWhere { Ansiblex.id notInList withRelation }
        }
    }

    override fun gatesRemoveRegion(regionId: Int, allianceId: Int) {
        transaction {
            // Find all from region
            val ids = mutableListOf<Long>()
            Ansiblex
                .select { Ansiblex.regionId eq regionId }
                .forEach { ids.add(it[Ansiblex.id].value) }

            // Delete relations
            AnsiblexAlliance.deleteWhere {
                AnsiblexAlliance.allianceId eq allianceId and (AnsiblexAlliance.ansiblexId inList ids)
            }

            // Delete Ansiblex gates without a relation
            val withRelation = mutableSetOf<Long>()
            AnsiblexAlliance.selectAll().forEach { withRelation.add(it[AnsiblexAlliance.ansiblexId].value) }
            Ansiblex.deleteWhere { Ansiblex.id notInList withRelation }
        }
    }

    override fun temporaryConnectionsGet(characterId: Int): List<MongoTemporaryConnection> {
        val mongoTemporaryConnection = mutableListOf<MongoTemporaryConnection>()

        transaction {
            TemporaryConnection.select { TemporaryConnection.characterId eq characterId }.forEach {
                mongoTemporaryConnection.add(
                    MongoTemporaryConnection(
                        system1Id = it[TemporaryConnection.system1Id],
                        system2Id = it[TemporaryConnection.system2Id],
                        characterId = it[TemporaryConnection.characterId],
                        system1Name = it[TemporaryConnection.system1Name],
                        system2Name = it[TemporaryConnection.system2Name],
                        created = date(it[TemporaryConnection.created])
                    )
                )
            }
        }

        return mongoTemporaryConnection
    }

    override fun temporaryConnectionStore(tempConnection: MongoTemporaryConnection) {
        transaction {
            val existing = TemporaryConnection.select {
                TemporaryConnection.system1Id eq tempConnection.system1Id and
                    (TemporaryConnection.system2Id eq tempConnection.system2Id)
            }.firstOrNull()

            if (existing == null) {
                TemporaryConnection.insert {
                    it[system1Id] = tempConnection.system1Id
                    it[system1Name] = tempConnection.system1Name
                    it[system2Id] = tempConnection.system2Id
                    it[system2Name] = tempConnection.system2Name
                    it[characterId] = tempConnection.characterId
                    it[created] = localDateTime(tempConnection.created)
                }
            } else {
                TemporaryConnection.update({ TemporaryConnection.id eq existing[TemporaryConnection.id] }) {
                    it[system1Id] = tempConnection.system1Id
                    it[system1Name] = tempConnection.system1Name
                    it[system2Id] = tempConnection.system2Id
                    it[system2Name] = tempConnection.system2Name
                    it[characterId] = tempConnection.characterId
                    it[created] = localDateTime(tempConnection.created)
                }
            }
        }
    }

    override fun temporaryConnectionsDeleteAllExpired() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        transaction {
            TemporaryConnection.deleteWhere {
                TemporaryConnection.created lessEq localDateTime(calendar.time)
            }
        }
    }

    override fun temporaryConnectionDelete(system1Id: Int, system2Id: Int, characterId: Int) {
        transaction {
            TemporaryConnection.deleteWhere {
                TemporaryConnection.system1Id eq system1Id and
                    (TemporaryConnection.system2Id eq system2Id and
                        (TemporaryConnection.characterId eq characterId))
            }
        }
    }

    override fun allianceAdd(allianceId: Int) {
        transaction {
            val existing = Alliance.select { Alliance.id eq allianceId }.firstOrNull()
            if (existing == null) {
                Alliance.insert {
                    it[id] = allianceId
                    it[updated] = localDateTime(Date(0))
                }
            }
        }
    }

    override fun allianceGet(allianceId: Int): MongoAlliance? {
        var result: MongoAlliance? = null
        transaction {
            val alliance = Alliance.select { Alliance.id eq allianceId }.firstOrNull()
            if (alliance != null) {
                result = MongoAlliance(id = alliance[Alliance.id].value, updated = date(alliance[Alliance.updated]))
            }
        }
        return result
    }

    override fun allianceUpdate(alliance: MongoAlliance) {
        transaction {
            val existing = Alliance.select { Alliance.id eq alliance.id }.firstOrNull()
            if (existing == null) {
                Alliance.insert {
                    it[id] = alliance.id
                    it[updated] = localDateTime(alliance.updated)
                }
            } else {
                Alliance.update({ Alliance.id eq existing[Alliance.id] }) {
                    it[id] = alliance.id
                    it[updated] = localDateTime(alliance.updated)
                }
            }
        }
    }

    override fun loginRegister(characterId: Int) {
        val today = LocalDate.now(ZoneOffset.UTC)
        transaction {
            val existing = Login.select {
                Login.characterId eq characterId and
                    (Login.year eq today.year and
                        (Login.month eq today.monthValue))
            }.firstOrNull()
            if (existing == null) {
                Login.insert {
                    it[Login.characterId] = characterId
                    it[year] = today.year
                    it[month] = today.monthValue
                    it[count] = 1
                }
            } else {
                Login.update({ Login.id eq existing[Login.id] }) {
                    it[count] = existing[count] + 1
                }
            }
        }
    }
}

private fun localDateTime(d: Date): LocalDateTime {
    return LocalDateTime.ofInstant(d.toInstant(), ZoneId.of("UTC"))
}

private fun date(dt: LocalDateTime): Date {
    return Date.from(dt.atZone(ZoneId.of("UTC")).toInstant())
}
