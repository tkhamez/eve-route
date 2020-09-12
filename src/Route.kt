package net.tkhamez.everoute

import net.tkhamez.everoute.data.*
import net.tkhamez.everoute.data.Graph
import java.util.*
import kotlin.collections.HashSet

class Route(
    ansiblexes: List<MongoAnsiblex>,
    temporaryConnections: List<MongoTemporaryConnection>,
    private val avoidedSystems: Set<Int>,
    private val removedConnections: Set<ConnectedSystems>
) {
    /**
     * Graph with ESI data of all systems with edges.
     */
    private val graph: Graph = net.tkhamez.everoute.Graph().getSystems()

    /**
     * Helper variable with all systems, including wormholes.
     */
    private val allSystems: MutableMap<Int, GraphSystem> = mutableMapOf()

    /**
     * Helper variable to keep a reference to all graph nodes, accessible by their system ID.
     */
    private val allNodes: MutableMap<Int, Node<GraphSystem>> = mutableMapOf()

    /**
     * Helper variable that keeps a reference to all Ansiblexes, accessible by their system ID.
     */
    private val allAnsiblexes: MutableMap<Int, MongoAnsiblex> = mutableMapOf()

    /**
     * Helper variable that keeps a reference to all temporary connections, accessible by their system ID.
     */
    private val allTemporaryConnections: MutableMap<Int, MongoTemporaryConnection> = mutableMapOf()

    private val centralNode: Node<GraphSystem>?

    init {
        // build node connections
        centralNode = buildNodes(graph)
        addGates(ansiblexes)
        addTempConnections(temporaryConnections)
    }

    fun find(from: String, to: String): List<Waypoint> {
        if (centralNode == null) { // should not happen
            return listOf()
        }

        // find start and end system
        val startSystem: GraphSystem? = findSystem(from)
        val endSystem: GraphSystem? = findSystem(to)
        if (startSystem == null || endSystem == null) {
            return listOf()
        }

        // Get start node, then from there search the end node - both should never be null here
        val startNode = allNodes[startSystem.id] ?: return listOf()
        val lastConnection = search(endSystem, startNode) ?: return listOf()

        // build path of waypoints from end to start
        val path: MutableList<Waypoint> = mutableListOf()
        var currentConnection: Connection<GraphSystem>? = lastConnection
        var previousConnection: Connection<GraphSystem>? = null
        while (currentConnection != null) {
            val ansiblex = if (previousConnection?.type == Waypoint.Type.Ansiblex) {
                allAnsiblexes[currentConnection.node.getValue().id]
            } else {
                null
            }
            var ansiblexId = ansiblex?.id
            if (previousConnection?.type == Waypoint.Type.Ansiblex && ansiblex == null) {
                // ESI returned only one Ansiblex from a pair - the one going the other direction
                ansiblexId = -1
            }
            val id = currentConnection.node.getValue().id
            path.add(Waypoint(
                systemId = currentConnection.node.getValue().id,
                systemName = currentConnection.node.getValue().name,
                targetSystem = previousConnection?.node?.getValue()?.name,
                wormhole = id in 31000000..32000000, // see https://docs.esi.evetech.net/docs/id_ranges.html
                systemSecurity = currentConnection.node.getValue().security,
                connectionType = previousConnection?.type,
                ansiblexId = ansiblexId,
                ansiblexName = ansiblex?.name
            ))
            previousConnection = currentConnection
            currentConnection = currentConnection.node.predecessor
        }

        return path.reversed()
    }

    /**
     * Creates and connects all nodes
     */
    private fun buildNodes(graph: Graph): Node<GraphSystem>? {
        graph.systems.forEach {
            allSystems[it.id] = it
        }

        var center: Node<GraphSystem>? = null

        graph.connections.forEach {
            val sourceNode = getNode(it[0])
            val targetNode = getNode(it[1])
            if (sourceNode != null && targetNode != null) { // system can be null if it is avoided
                sourceNode.connect(targetNode, Waypoint.Type.Stargate)

                if (sourceNode.getValue().name == "Skarkon") {
                    center = sourceNode
                }
            }
        }

        return center
    }

    private fun addGates(ansiblexes: List<MongoAnsiblex>) {
        ansiblexes.forEach { gate ->
            allAnsiblexes[gate.solarSystemId] = gate

            // find end system ID (full gate name e.g.: "5ELE-A » AZN-D2 - Easy Route")
            val systemNames = gate.name.substring(0, gate.name.indexOf(" - ")) // e.g. "5ELE-A » AZN-D2"
            val endSystemName = systemNames.substring(systemNames.indexOf(" » ") + 3)
            val endSystemId = findSystem(endSystemName)?.id

            // connect nodes
            val startSystemNode = allNodes[gate.solarSystemId]
            val endSystemNode = allNodes[endSystemId]
            if (startSystemNode != null && endSystemNode != null) { // system can be null if it is avoided
                if (! isRemoved(startSystemNode.getValue().name, endSystemNode.getValue().name)) {
                    startSystemNode.connect(endSystemNode, Waypoint.Type.Ansiblex)
                }
            }
        }
    }

    private fun addTempConnections(temporaryConnections: List<MongoTemporaryConnection>) {
        temporaryConnections.forEach { connection ->
            allTemporaryConnections[connection.system1Id] = connection
            allTemporaryConnections[connection.system2Id] = connection

            // get/create and connect nodes
            val startSystemNode = getNode(connection.system1Id)
            val endSystemNode = getNode(connection.system2Id)
            if (startSystemNode != null && endSystemNode != null) { // system can be null if it is avoided
                if (! isRemoved(startSystemNode.getValue().name, endSystemNode.getValue().name)) {
                    startSystemNode.connect(endSystemNode, Waypoint.Type.Temporary)
                }
            }
        }
    }

    private fun isRemoved(startSystemName: String, endSystemName: String): Boolean {
        var removed = false
        for (removedConnection in removedConnections) {
            if (
                (
                        removedConnection.system1 == startSystemName &&
                                removedConnection.system2 == endSystemName
                        ) || (
                        removedConnection.system1 == endSystemName &&
                                removedConnection.system2 == startSystemName
                        )
            ) {
                removed = true
                break
            }
        }
        return removed
    }

    private fun getNode(systemId: Int): Node<GraphSystem>? {
        if (avoidedSystems.contains(systemId)) {
            return null
        }

        if (allNodes[systemId] == null) {
            val sourceSystem = allSystems[systemId]
            if (sourceSystem != null) {
                allNodes[systemId] = Node(sourceSystem)
            }
        }
        return allNodes[systemId]
    }

    private fun findSystem(systemName: String): GraphSystem? {
        for (system in graph.systems) {
            if (system.name.toLowerCase() == systemName.toLowerCase()) {
                return system
            }
        }
        return null
    }

    /**
     * Breadth first search that keeps the predecessor of each vertex.
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

    private class Node<T>(private val value: T) {
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

            // This does *not* add the same node twice, even if the connection object is different
            connections.add(Connection(node, type))
            node.connections.add(Connection(this, type))
        }
    }

    private data class Connection<T>(
        val node: Node<T>,
        val type: Waypoint.Type? = null
    )

    data class Waypoint(
        val systemId: Int,
        val systemName: String,
        val targetSystem: String?,
        val wormhole: Boolean,
        val systemSecurity: Double,
        val connectionType: Type?, // the type of the outgoing connection, null for the end system
        val ansiblexId: Long?,
        val ansiblexName: String?
    ) {
        enum class Type { Stargate, Ansiblex, Temporary }
    }
}
