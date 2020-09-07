package net.tkhamez.everoute

import com.google.gson.Gson
import net.tkhamez.everoute.data.Graph
import net.tkhamez.everoute.data.System
import java.io.File

class Graph {
    fun getSystems(): Graph {
        // null for DEV mode (run task), not sure why, it works for Route.frontend()
        val resource = javaClass.getResource("/graph.json")

        val graphJson = resource?.readText() ?: File("resources/graph.json").readText()
        return Gson().fromJson(graphJson, Graph::class.java)
    }

    fun findSystem(systemId: Int): System? {
        for (system in getSystems().systems) {
            if (system.id == systemId) {
                return system
            }
        }
        return null
    }

    fun findSystem(systemName: String): System? {
        for (system in getSystems().systems) {
            if (system.name == systemName) {
                return system
            }
        }
        return null
    }
}