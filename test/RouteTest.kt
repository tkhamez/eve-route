package net.tkhamez.everoute

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {

    @Test
    fun testFind() {
        val route = Route(listOf(), listOf(), setOf(30003504), setOf()).find("jita", "amarr")
        assertEquals(24, route.size)
    }
}
