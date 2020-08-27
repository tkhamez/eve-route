package net.tkhamez.everoute.scripts

import com.google.gson.Gson
import net.tkhamez.everoute.data.Edge
import net.tkhamez.everoute.data.Graph
import net.tkhamez.everoute.data.Position
import net.tkhamez.everoute.data.System as DataSystem
import java.io.File
import kotlin.math.round

val esiData = readData()
val graph = buildGraph(esiData)
writeJsonFile(graph)

fun readData(): Data {
    val regionDenyList = listOf(
        "A821-A", "J7HZ-F", "UUA-F4", // unreachable normal regions
        //"A-R00001", "A-R00002", "A-R00003", // Wormholes
        "ADR01", "ADR02", "ADR03", "ADR04", "ADR05", // Abyssal Deadspace
        "PR-01" // unknown
    )
    val dataPath = "esi-data/json/universe/"

    val esiData = Data()

    val regionsJson = File(dataPath + "regions/regions.json").readText()
    val regions = Gson().fromJson(regionsJson, Array<Region>::class.java)
    for (region in regions) {
        if (regionDenyList.indexOf(region.name) != -1) {
            continue
        }
        esiData.regions.add(region)

        val systemJson = File(dataPath + "systems/" + region.name + "-systems.json").readText()
        val systems = Gson().fromJson(systemJson, Array<System>::class.java)
        for (system in systems) {
            esiData.systems.add(system)
        }

        val stargateJson = File(dataPath + "stargates/" + region.name + "-stargates.json").readText()
        val stargates = Gson().fromJson(stargateJson, Array<Stargate>::class.java)
        for (stargate in stargates) {
            esiData.stargates.add(stargate)
        }
    }

    return esiData
}

fun buildGraph(data: Data): Graph {
    val graph = Graph()

    for (system in data.systems) {
        val security = if (system.securityStatus > 0 && system.securityStatus < 0.05) {
            0.1
        } else if (system.securityStatus <= 0.0) {
            round(system.securityStatus * 100) / 100
        } else {
            round(system.securityStatus * 10) / 10
        }
        graph.systems.add(DataSystem(system.id, system.name, security, system.position))
    }

    val uniqueEdges = mutableListOf<String>()
    for (stargate in data.stargates) {
        val uniqueEdge = if (stargate.systemId < stargate.destination.systemId) {
            "${stargate.systemId}-${stargate.destination.systemId}"
        } else {
            "${stargate.destination.systemId}-${stargate.systemId}"
        }
        if (uniqueEdges.indexOf(uniqueEdge) == -1) {
            uniqueEdges.add(uniqueEdge)
            graph.edges.add(Edge(stargate.systemId, stargate.destination.systemId))
        }
    }

    return graph
}

fun writeJsonFile(graph: Graph) {
    val json = Gson().toJson(graph)
    val file = File("resources/graph.json")
    file.writeText(json)
}

data class Destination(val systemId: Int)
data class Stargate(val systemId: Int, val destination: Destination)
data class System(val id: Int, val name: String, val securityStatus: Double, val position: Position)
data class Region(val name: String)

data class Data(
    val regions: MutableList<Region> = mutableListOf(),
    val systems: MutableList<System> = mutableListOf(),
    val stargates: MutableList<Stargate> = mutableListOf()
)
