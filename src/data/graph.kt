/**
 * EVE Map
 */

package net.tkhamez.everoute.data

data class Position(
    val x: Double,
    val y: Double,
    val z: Double
)

data class System(
    val id: Int,
    val name: String,
    val security: Double,
    val position: Position
)

data class Edge(
    val source: Int,
    val target: Int
)

data class Graph(
    val systems: MutableList<System> = mutableListOf(),
    val edges: MutableList<Edge> = mutableListOf()
)
