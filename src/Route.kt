package net.tkhamez.everoute

import net.tkhamez.everoute.data.ConnectedSystems
import net.tkhamez.everoute.data.GraphSystem
import net.tkhamez.everoute.data.MongoAnsiblex
import net.tkhamez.everoute.data.MongoTemporaryConnection
import java.util.*
import kotlin.collections.HashSet

class Route(
    ansiblexes: List<MongoAnsiblex>,
    temporaryConnections: List<MongoTemporaryConnection>,
    private val avoidedSystems: Set<Int>,
    private val removedConnections: Set<ConnectedSystems>
) {
    private val graphHelper = GraphHelper()

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

    init {
        // build node connections
        buildNodes()
        addGates(ansiblexes)
        addTempConnections(temporaryConnections)
    }

    fun find(from: String, to: String): List<List<Waypoint>> {
        // find start and end system
        val startSystem = graphHelper.findSystem(from)
        val endSystem = graphHelper.findSystem(to)
        if (startSystem == null || endSystem == null) {
            return listOf()
        }

        // Get start node, then from there search the end node
        val startNode = allNodes[startSystem.id] ?: return listOf()
        val paths = search(endSystem, startNode)

        // build waypoints
        val foundRoutes: MutableList<List<Waypoint>> = mutableListOf()
        paths.forEach {
            foundRoutes.add(buildWaypoints(it))
        }

        return foundRoutes
    }

    /**
     * Creates and connects all nodes
     */
    private fun buildNodes() {
        graphHelper.getGraph().systems.forEach {
            allSystems[it.id] = it
        }

        graphHelper.getGraph().connections.forEach {
            val sourceNode = getNode(it[0])
            val targetNode = getNode(it[1])
            if (sourceNode != null && targetNode != null) { // system can be null if it is avoided
                sourceNode.connect(targetNode, Waypoint.Type.Stargate)
            }
        }
    }

    private fun addGates(ansiblexes: List<MongoAnsiblex>) {
        ansiblexes.forEach { gate ->
            allAnsiblexes[gate.solarSystemId] = gate

            val endSystemId = graphHelper.getEndSystem(gate)?.id

            // connect nodes
            val startSystemNode = allNodes[gate.solarSystemId]
            val endSystemNode = allNodes[endSystemId]
            if (startSystemNode != null && endSystemNode != null) { // system can be null if it is avoided
                if (!isRemoved(startSystemNode.getValue().name, endSystemNode.getValue().name)) {
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
                if (!isRemoved(startSystemNode.getValue().name, endSystemNode.getValue().name)) {
                    startSystemNode.connect(endSystemNode, Waypoint.Type.Temporary)
                }
            }
        }
    }

    private fun isRemoved(startSystemName: String, endSystemName: String): Boolean {
        var removed = false
        for (removedConnection in removedConnections) {
            if (
                (removedConnection.system1 == startSystemName && removedConnection.system2 == endSystemName) ||
                (removedConnection.system1 == endSystemName && removedConnection.system2 == startSystemName)
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

    private fun <T> search(value: T, start: Node<T>): List<List<Connection<T>>> {
        val foundPaths: MutableList<List<Connection<T>>> = mutableListOf()
        val visitedPaths: MutableList<List<Connection<T>>> = mutableListOf()

        // Breadth first search that finds multiple paths as long as they don't share any nodes
        // (except start and end of course)
        var lastRouteLength = 0
        val alreadyVisited: MutableSet<Connection<T>> = HashSet()
        val queue: Queue<MutableList<Connection<T>>> = ArrayDeque()
        queue.add(mutableListOf(Connection(node = start)))
        while (!queue.isEmpty()) {
            val currentPath = queue.remove()
            val currentConnection = currentPath.last()
            if (lastRouteLength > 0 && currentPath.size > lastRouteLength) {
                // Already found a shorter path.
                // This is possible because adjacent nodes are added to the queue before all of them were checked
                continue
            }
            if (currentConnection.node.getValue() == value) {
                foundPaths.add(currentPath)
                lastRouteLength = currentPath.size
            } else if (currentConnection in alreadyVisited) {
                // Possible start of an alternative path with common nodes
                visitedPaths.add(currentPath)
            } else if (lastRouteLength == 0 || currentPath.size < lastRouteLength) {
                currentConnection.node.getConnections().forEach { connection ->
                    val newPath = currentPath.toMutableList()
                    newPath.add(connection)
                    queue.add(newPath)
                }
                alreadyVisited.add(currentConnection)
            }
        }

        // Check previously skipped paths with common nodes for alternative routes
        val additionalPaths: MutableList<List<Connection<T>>> = mutableListOf()
        visitedPaths.forEach {
            val lastConnection = it.last()
            for (foundPath in foundPaths) {
                if (lastConnection in foundPath && foundPath.indexOf(lastConnection) == it.size - 1) {
                    // The last node is at the same position in the other list.
                    // Add the remaining nodes to a new path.
                    val newPath = it.toMutableList() + foundPath.drop(it.size)
                    additionalPaths.add(newPath)
                }
            }
        }

        return foundPaths + additionalPaths
    }

    private fun <T> buildWaypoints(path: List<Connection<T>>): List<Waypoint> {
        val waypoints: MutableList<Waypoint> = mutableListOf()

        val listIterator = path.reversed().listIterator()
        var previousConnection: Connection<T>? = null
        var previousSystem: GraphSystem? = null
        var ansiblex: MongoAnsiblex? = null
        while (listIterator.hasNext()) {
            val connection = listIterator.next()
            val system: GraphSystem = connection.node.getValue() as GraphSystem

            if (previousConnection != null) {
                previousSystem = previousConnection.node.getValue() as GraphSystem
            }

            if (previousConnection?.type == Waypoint.Type.Ansiblex) {
                ansiblex = allAnsiblexes[system.id]
            }
            var ansiblexId = ansiblex?.id
            if (previousConnection?.type == Waypoint.Type.Ansiblex && ansiblex == null) {
                // ESI returned only one Ansiblex from a pair - the one going the other direction
                ansiblexId = -1
            }

            waypoints.add(Waypoint(
                systemId = system.id,
                systemName = system.name,
                targetSystem = previousSystem?.name,
                wormhole = system.id in 31000000..32000000, // see https://docs.esi.evetech.net/docs/id_ranges.html
                systemSecurity = system.security,
                connectionType = previousConnection?.type,
                ansiblexId = ansiblexId,
                ansiblexName = ansiblex?.name
            ))

            previousConnection = connection
        }

        return waypoints.reversed()
    }

    private class Node<T>(private val value: T) {
        private val connections: MutableSet<Connection<T>> = HashSet() // mutableSetOf() seems to be ordered

        fun getValue(): T {
            return value
        }

        fun getConnections(): MutableSet<Connection<T>> {
            return connections
        }

        fun connect(node: Node<T>, type: Waypoint.Type) {
            if (this !== node) {
                // This does *not* add the same node twice, even if the connection object is different
                connections.add(Connection(node, type))
                node.connections.add(Connection(this, type))
            }
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
