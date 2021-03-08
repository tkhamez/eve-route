package net.tkhamez.everoute

import com.google.gson.Gson
import net.tkhamez.everoute.data.Graph
import net.tkhamez.everoute.data.GraphSystem
import java.io.File

class GraphHelper {
    private var graph: Graph? = null

    fun getGraph(): Graph {
        if (graph is Graph) {
            return graph as Graph
        }

        // null for DEV mode (run task), not sure why, it works for Route.frontend()
        val resource = javaClass.getResource("/graph.json")

        val graphJson = resource?.readText() ?: File("resources/graph.json").readText()
        graph = Gson().fromJson(graphJson, Graph::class.java)

        return graph as Graph
    }

    fun findSystem(systemId: Int): GraphSystem? {
        for (system in getGraph().systems) {
            if (system.id == systemId) {
                return system
            }
        }
        return null
    }

    fun findSystem(systemName: String): GraphSystem? {
        for (system in getGraph().systems) {
            if (system.name.equals(systemName, ignoreCase = true)) {
                return system
            }
        }
        return null
    }

    /**
     * Find start system from Ansiblex name
     */
    fun getStartSystem(ansiblexName: String): GraphSystem? {
        // name is e.g. "5ELE-A » AZN-D2 - Easy Route"
        val startSystemName = ansiblexName.substring(0, ansiblexName.indexOf(" » "))
        return findSystem(startSystemName)
    }

    /**
     * Find end system from Ansiblex name (full gate name e.g.: "5ELE-A » AZN-D2 - Easy Route")
     */
    fun getEndSystem(ansiblexName: String): GraphSystem? {
        val systemNames = ansiblexName.substring(0, ansiblexName.indexOf(" - ")) // e.g. "5ELE-A » AZN-D2"
        val endSystemName = systemNames.substring(systemNames.indexOf(" » ") + 3)
        return findSystem(endSystemName)
    }
}
