/**
 * EVE Map
 */

package net.tkhamez.everoute.data

data class GraphPosition(
    val x: Double,
    val y: Double,
    val z: Double
)

data class GraphSystem(
    val id: Int,
    val name: String,
    val security: Double,
    val position: GraphPosition,
    val regionId: Int,
)

data class Graph(
    val systems: MutableList<GraphSystem> = mutableListOf(),
    val connections: MutableList<IntArray> = mutableListOf(),
    val regions: MutableMap<Int, String> = mutableMapOf(),
)
