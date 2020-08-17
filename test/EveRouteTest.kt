package net.tkhamez.everoute

import kotlin.test.Test
import kotlin.test.assertEquals

class EveRouteTest {

    @Test
    fun testFind() {
        val route = EveRoute().find("jita", "amarr")
        assertEquals(10, route.size)
    }
}
