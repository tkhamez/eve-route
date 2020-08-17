package net.tkhamez.everoute

import com.google.gson.Gson
import net.tkhamez.everoute.data.Gate
import net.tkhamez.everoute.data.Graph
import net.tkhamez.everoute.data.System
import java.io.File
import java.util.*
import kotlin.collections.HashSet

class EveRoute(ansiblexes: List<Gate>) {
    /**
     * Graph with ESI data of all systems with edges.
     */
    private val graph: Graph

    /**
     * Helper variable to keep a reference to all nodes, accessible by their system ID.
     */
    private val allNodes: MutableMap<Int, Node<System>> = mutableMapOf()

    val centralNode: Node<System>?

    init {
        // null for DEV mode (run task), not sure why, it works for Route.frontend()
        val resource = javaClass.getResource("/graph.json")

        val graphJson = resource?.readText() ?: File("resources/graph.json").readText()
        graph = Gson().fromJson(graphJson, Graph::class.java)

        // build node connections
        centralNode = buildNodes(graph)
        addGates(ansiblexes)
    }

    fun find(from: String, to: String): List<Waypoint> {
        if (centralNode == null) { // should not happen
            return listOf()
        }

        // find start and end system
        val startSystem: System? = findSystem(from)
        val endSystem: System? = findSystem(to)
        if (startSystem == null || endSystem == null) {
            return listOf()
        }

        // Get start node, then from there search the end node - both should never be null here
        val startNode = allNodes[startSystem.id] ?: return listOf()
        val lastConnection = search(endSystem, startNode) ?: return listOf()

        // build path of waypoints from end to start
        val path: MutableList<Waypoint> = mutableListOf()
        var currentConnection: Connection<System>? = lastConnection
        while (currentConnection != null) {
            path.add(Waypoint(system = currentConnection.node.getValue(), type = currentConnection.type))
            currentConnection = currentConnection.node.predecessor
        }

        return path.reversed()
    }

    /**
     * Creates and connects all nodes
     */
    private fun buildNodes(graph: Graph): Node<System>? {
        val allSystems: MutableMap<Int, System> = mutableMapOf()
        graph.systems.forEach {
            allSystems[it.id] = it
        }

        fun getNode(systemId: Int): Node<System>? {
            if (allNodes[systemId] == null) {
                val sourceSystem = allSystems[systemId]
                if (sourceSystem != null) {
                    allNodes[systemId] = Node(sourceSystem)
                }
            }
            return allNodes[systemId]
        }

        var center: Node<System>? = null

        graph.edges.forEach {
            val sourceNode = getNode(it.source)
            val targetNode = getNode(it.target)
            if (sourceNode != null && targetNode != null) { // should always be true
                sourceNode.connect(targetNode, Waypoint.Type.Stargate)

                if (sourceNode.getValue().name == "Skarkon") {
                    center = sourceNode
                }
            }
        }

        return center
    }

    private fun addGates(gates: List<Gate>) {
        gates.forEach { gate ->
            // find end system ID (full gate name e.g.: "5ELE-A » AZN-D2 - Easy Route")
            val systemNames = gate.name.substring(0, gate.name.indexOf(" - ")) // e.g. "5ELE-A » AZN-D2"
            val endSystemName = systemNames.substring(systemNames.indexOf(" » ") + 3)
            val endSystemId = findSystem(endSystemName)?.id

            // connect nodes
            val startSystemNode = allNodes[gate.solarSystemId]
            val endSystemNode = allNodes[endSystemId]
            if (startSystemNode != null && endSystemNode != null) { // should never be null
                startSystemNode.connect(endSystemNode, Waypoint.Type.Ansiblex)
            }
        }
    }

    private fun findSystem(systemName: String): System? {
        for (system in graph.systems) {
            if (system.name.toLowerCase() == systemName.toLowerCase()) {
                return system
            }
        }
        return null
    }

    /**
     * Breadth first search that keeps the predecessor of each vertex
     */
    private fun <T> search(value: T, start: Node<T>): Connection<T>? {
        // clear predecessors
        allNodes.forEach { (_, value) -> value.predecessor = null }

        val queue: Queue<Connection<T>> = ArrayDeque()
        queue.add(Connection(node = start))
        var currentConnection: Connection<T>
        val alreadyVisited: MutableSet<Connection<T>> = HashSet()
        while (!queue.isEmpty()) {
            currentConnection = queue.remove()
            if (currentConnection.node.getValue() == value) {
                return currentConnection
            } else {
                currentConnection.node.getConnections().forEach { connection ->
                if (connection.node !== start && connection.node.predecessor == null) {
                        connection.node.predecessor = currentConnection
                    }
                    queue.add(connection)
                }
                alreadyVisited.add(currentConnection)
                queue.removeAll(alreadyVisited)
            }
        }
        return null
    }

    class Node<T>(private val value: T) {
        var predecessor: Connection<T>? = null

        private val connections: MutableSet<Connection<T>> = HashSet() // mutableSetOf() seems to be ordered

        fun getValue(): T {
            return value
        }

        fun getConnections(): MutableSet<Connection<T>> {
            return connections
        }

        fun connect(node: Node<T>, type: Waypoint.Type) {
            require(!(this === node)) {
                "Can't connect node to itself"
            }
            connections.add(Connection(node, type))
            node.connections.add(Connection(this, type))
        }
    }

    data class Connection<T>(
        val node: Node<T>,
        val type: Waypoint.Type? = null
    )

    data class Waypoint(
        val system: System,
        val type: Type? // the type of the incoming connection, null for the start system
    ) {
        enum class Type { Stargate, Ansiblex }
    }
}
