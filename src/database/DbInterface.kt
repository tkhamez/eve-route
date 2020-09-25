package net.tkhamez.everoute.database

import net.tkhamez.everoute.data.MongoAlliance
import net.tkhamez.everoute.data.MongoAnsiblex
import net.tkhamez.everoute.data.MongoTemporaryConnection

interface DbInterface {
    fun migrate()

    fun gatesGet(allianceId: Int): List<MongoAnsiblex>

    fun gateStore(ansiblex: MongoAnsiblex, allianceId: Int)

    fun gatesRemoveOther(ansiblexes: List<MongoAnsiblex>, allianceId: Int)

    fun temporaryConnectionsGet(characterId: Int): List<MongoTemporaryConnection>

    fun temporaryConnectionStore(tempConnection: MongoTemporaryConnection)

    fun temporaryConnectionsDeleteAllExpired()

    fun temporaryConnectionDelete(system1Id: Int, system2Id: Int, characterId: Int)

    fun allianceGet(allianceId: Int): MongoAlliance?

    fun allianceUpdate(alliance: MongoAlliance)
}
