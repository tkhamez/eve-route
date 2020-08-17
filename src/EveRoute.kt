package net.tkhamez.everoute

import com.google.gson.Gson
import net.tkhamez.everoute.data.Graph
import net.tkhamez.everoute.data.System
import java.io.File
import java.util.*
import kotlin.collections.HashSet

class EveRoute {
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
    }

    fun find(from: String, to: String): List<System> {
        if (centralNode == null) { // should not happen
            return listOf()
        }

        // find start and end system
        var startSystem: System? = null
        var endSystem: System? = null
        for (system in graph.systems) {
            if (system.name.toLowerCase() == from.toLowerCase()) {
                startSystem = system
            }
            if (system.name.toLowerCase() == to.toLowerCase()) {
                endSystem = system
            }
            if (startSystem != null && endSystem != null) {
                break
            }
        }
        if (startSystem == null || endSystem == null) {
            return listOf()
        }

        // Get start node, then from there search the end node - both should never be null here
        val startNode = allNodes[startSystem.id] ?: return listOf()
        val endNode = search(endSystem, startNode) ?: return listOf()

        // build path
        val path: MutableList<System> = mutableListOf()
        var currentNode = endNode
        while (currentNode.predecessor != null) {
            path.add(currentNode.getValue())
            currentNode = currentNode.predecessor!! // should never be null here
        }
        path.add(currentNode.getValue()) // startNode === currentNode here

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

        var center: Node<System>? = null

        graph.edges.forEach {
            var sourceNode = allNodes[it.source]
            if (sourceNode == null) {
                val sourceSystem = allSystems[it.source]
                if (sourceSystem != null) {
                    sourceNode = Node(sourceSystem)
                    allNodes[it.source] = sourceNode
                }
            }

            var targetNode = allNodes[it.target]
            if (targetNode == null) {
                val targetSystem = allSystems[it.target]
                if (targetSystem != null) {
                    targetNode = Node(targetSystem)
                    allNodes[it.target] = targetNode
                }
            }

            if (sourceNode != null && targetNode != null) { // should always be true
                sourceNode.connect(targetNode)

                if (sourceNode.getValue().name == "Skarkon") {
                    center = sourceNode
                }
            }
        }

        return center
    }

    /**
     * Breadth first search that keeps the predecessor of each vertex
     */
    private fun <T> search(value: T, start: Node<T>): Node<T>? {
        // clear predecessors
        allNodes.forEach { (_, value) -> value.predecessor = null }

        val queue: Queue<Node<T>> = ArrayDeque()
        queue.add(start)
        var currentNode: Node<T>
        val alreadyVisited: MutableSet<Node<T>> = HashSet()
        while (!queue.isEmpty()) {
            currentNode = queue.remove()
            if (currentNode.getValue() == value) {
                return currentNode
            } else {
                currentNode.getNeighbors().forEach {
                    if (it !== start && it.predecessor == null) {
                        it.predecessor = currentNode
                    }
                }
                alreadyVisited.add(currentNode)
                queue.addAll(currentNode.getNeighbors())
                queue.removeAll(alreadyVisited)
            }
        }
        return null
    }

    class Node<T>(private val value: T) {
        var predecessor: Node<T>? = null

        private val neighbors: MutableSet<Node<T>>

        init {
            neighbors = java.util.HashSet()
        }

        fun getValue(): T {
            return value
        }

        fun getNeighbors(): MutableSet<Node<T>> {
            return neighbors
        }

        fun connect(node: Node<T>) {
            require(!(this === node)) {
                "Can't connect node to itself"
            }
            neighbors.add(node)
            node.neighbors.add(this)
        }
    }
}
