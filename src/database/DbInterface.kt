package net.tkhamez.everoute.database

import net.tkhamez.everoute.data.MongoAlliance
import net.tkhamez.everoute.data.MongoAnsiblex
import net.tkhamez.everoute.data.MongoTemporaryConnection

interface DbInterface {
    fun migrate()

    fun gatesGet(allianceId: Int): List<MongoAnsiblex>

    fun gateStore(ansiblex: MongoAnsiblex, allianceId: Int)

    /**
     * Delete one gate for one alliance.
     *
     * Returns true if gate for the alliance was deleted of does not exist.
     */
    fun gateDelete(ansiblexId: Long, allianceId: Int): Boolean

    /**
     * Delete all Ansiblexes that were added by an ESI search.
     */
    fun gatesRemoveSourceESI(allianceId: Int)

    /**
     * Remove all Ansiblexes from one region and alliance
     */
    fun gatesRemoveRegion(regionId: Int, allianceId: Int)

    fun temporaryConnectionsGet(characterId: Int): List<MongoTemporaryConnection>

    fun temporaryConnectionStore(tempConnection: MongoTemporaryConnection)

    fun temporaryConnectionsDeleteAllExpired()

    fun temporaryConnectionDelete(system1Id: Int, system2Id: Int, characterId: Int)

    fun allianceAdd(allianceId: Int)

    fun allianceGet(allianceId: Int): MongoAlliance?

    fun allianceUpdate(alliance: MongoAlliance)

    fun loginRegister(characterId: Int)
}
