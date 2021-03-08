package net.tkhamez.everoute.scripts

import com.google.gson.Gson
import net.tkhamez.everoute.data.Graph
import net.tkhamez.everoute.data.GraphPosition
import net.tkhamez.everoute.data.GraphSystem
import java.io.File
import kotlin.math.round

val dataPath = "esi-data/json/universe/"

val graph = readData()
writeJsonFile(graph)

fun readData(): Graph {
    val regionDenyList = listOf(
        "A821-A", "J7HZ-F", "UUA-F4", // unreachable normal regions
        //"A-R00001", "A-R00002", "A-R00003", // Wormholes
        "ADR01", "ADR02", "ADR03", "ADR04", "ADR05", // Abyssal Deadspace
        "PR-01" // unknown
    )
    // const regionDenyListSubString = '-R00'; Wormholes

    val graph = Graph()

    val regionsJson = File(dataPath + "regions/regions.json").readText()
    val regions = Gson().fromJson(regionsJson, Array<Region>::class.java)
    for (region in regions) {
        if (regionDenyList.indexOf(region.name) != -1) {
            continue
        }

        val systemJson = File(dataPath + "systems/" + region.name + "-systems.json").readText()
        val systems = Gson().fromJson(systemJson, Array<System>::class.java)
        for (system in systems) {
            val security = if (system.securityStatus > 0 && system.securityStatus < 0.05) {
                0.1
            } else if (system.securityStatus <= 0.0) {
                round(system.securityStatus * 100) / 100
            } else {
                round(system.securityStatus * 10) / 10
            }
            graph.systems.add(GraphSystem(
                id = system.id,
                name = system.name,
                security = security,
                position = system.position,
                regionId = region.id
            ))
            if (!graph.regions.containsKey(region.id)) {
                graph.regions[region.id] = region.name
            }
        }

        val stargateJson = File(dataPath + "stargates/" + region.name + "-stargates.json").readText()
        val stargates = Gson().fromJson(stargateJson, Array<Stargate>::class.java)
        val uniqueEdges = mutableListOf<String>()
        for (stargate in stargates) {
            val uniqueEdge = if (stargate.systemId < stargate.destination.systemId) {
                "${stargate.systemId}-${stargate.destination.systemId}"
            } else {
                "${stargate.destination.systemId}-${stargate.systemId}"
            }
            if (uniqueEdges.indexOf(uniqueEdge) == -1) {
                uniqueEdges.add(uniqueEdge)
                graph.connections.add(intArrayOf(stargate.systemId, stargate.destination.systemId))
            }
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
data class System(
    val id: Int,
    val name: String,
    val constellationId: Int,
    val securityStatus: Double,
    val position: GraphPosition
)
data class Region(val id: Int, val name: String)
