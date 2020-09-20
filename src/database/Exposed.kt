package net.tkhamez.everoute.database

import net.tkhamez.everoute.data.MongoAlliance
import net.tkhamez.everoute.data.MongoAnsiblex
import net.tkhamez.everoute.data.MongoTemporaryConnection
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class Exposed(uri: String): DbInterface {
    object Ansiblex: IntIdTable() {
        val ansiblexId = long("ansiblexId")
        val allianceId = integer("allianceId")
        val name = varchar("name", 255)
        val solarSystemId = integer("solarSystemId")
        init {
            index(true, ansiblexId, allianceId) // unique index
        }
    }

    object TemporaryConnection: IntIdTable() {
        val system1Id = integer("system1Id")
        val system2Id = integer("system2Id")
        val characterId = integer("characterId")
        val system1Name = varchar("system1Name", 255)
        val system2Name = varchar("system2Name", 255)
        val created = datetime("created")
        init {
            index(true, system1Id, system2Id, characterId)
        }
    }

    object Alliance: Table() {
        val id = integer("id")
        val updated = datetime("updated")
        override val primaryKey = PrimaryKey(id, name = "PK_Alliance")
    }

    init {
        val url: String
        var user = ""
        var password = ""
        if (uri.startsWith("jdbc:postgresql:") || uri.startsWith("jdbc:mysql:") || uri.startsWith("jdbc:mariadb:")) {
            // e.g. jdbc:mysql://eve_route:password@localhost:3306/eve_route?serverTimezone=UTC
            val scheme = uri.substring(0, uri.indexOf("://") + 3)
            val hostAndRest = uri.substring(uri.indexOf("@") + 1)
            url = "$scheme$hostAndRest"
            val userAndPassword = uri.substring(uri.indexOf("://") + 3, uri.indexOf("@"))
            user = userAndPassword.substring(0, userAndPassword.indexOf(":"))
            password = userAndPassword.substring(userAndPassword.indexOf(":") + 1)
        } else if (uri.startsWith("jdbc:sqlite:") || uri.startsWith("jdbc:h2:")) {
            url = uri
        } else {
            throw Exception("Database not supported.")
        }

        Database.connect(
            url = url,
            user = URLDecoder.decode(user, StandardCharsets.UTF_8.name()),
            password = URLDecoder.decode(password, StandardCharsets.UTF_8.name()),
        )

        transaction {
            SchemaUtils.create(Ansiblex, TemporaryConnection, Alliance)
        }
    }

    override fun gatesGet(allianceId: Int): List<MongoAnsiblex> {
        val gates = mutableListOf<MongoAnsiblex>()

        transaction {
            Ansiblex.select { Ansiblex.allianceId eq allianceId }.forEach {
                gates.add(
                    MongoAnsiblex(
                        id = it[Ansiblex.ansiblexId],
                        allianceId = it[Ansiblex.allianceId],
                        name = it[Ansiblex.name],
                        solarSystemId = it[Ansiblex.solarSystemId],
                    )
                )
            }
        }

        return gates
    }

    override fun gateStore(ansiblex: MongoAnsiblex, allianceId: Int) {
        transaction {
            val existing = Ansiblex.select {
                Ansiblex.ansiblexId eq ansiblex.id and (Ansiblex.allianceId eq allianceId)
            }.firstOrNull()
            if (existing == null) {
                Ansiblex.insert {
                    it[ansiblexId] = ansiblex.id
                    it[Ansiblex.allianceId] = ansiblex.allianceId
                    it[name] = ansiblex.name
                    it[solarSystemId] = ansiblex.solarSystemId
                }
            } else {
                Ansiblex.update({ Ansiblex.id eq existing[Ansiblex.id] }) {
                    it[ansiblexId] = ansiblex.id
                    it[Ansiblex.allianceId] = ansiblex.allianceId
                    it[name] = ansiblex.name
                    it[solarSystemId] = ansiblex.solarSystemId
                }
            }
        }
    }

    override fun gatesRemoveOther(ansiblexes: List<MongoAnsiblex>, allianceId: Int) {
        val ids: MutableList<Long> = mutableListOf()
        ansiblexes.forEach { ids.add(it.id) }
        transaction {
            Ansiblex.select {
                Ansiblex.ansiblexId notInList ids and (Ansiblex.allianceId eq allianceId)
            }.forEach {
                Ansiblex.deleteWhere { Ansiblex.id eq it[Ansiblex.id] }
            }
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

    override fun allianceGet(allianceId: Int): MongoAlliance? {
        var result: MongoAlliance? = null
        transaction {
            val alliance = Alliance.select { Alliance.id eq allianceId }.firstOrNull()
            if (alliance != null) {
                result = MongoAlliance(id = alliance[Alliance.id], updated = date(alliance[Alliance.updated]))
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
}

private fun localDateTime(d: Date): LocalDateTime {
    return LocalDateTime.ofInstant(d.toInstant(), ZoneId.of("UTC"))
}

private fun date(dt: LocalDateTime): Date {
    return Date.from(dt.atZone(ZoneId.of("UTC")).toInstant())
}
