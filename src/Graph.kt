package net.tkhamez.everoute

import com.google.gson.Gson
import net.tkhamez.everoute.data.Graph
import java.io.File

class Graph {
    fun getSystems(): Graph {
        // null for DEV mode (run task), not sure why, it works for Route.frontend()
        val resource = javaClass.getResource("/graph.json")

        val graphJson = resource?.readText() ?: File("resources/graph.json").readText()
        return Gson().fromJson(graphJson, Graph::class.java)
    }
}