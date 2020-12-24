package net.tkhamez.everoute

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {

    @Test
    fun testFind() {
        val route = Route(listOf(), listOf(), setOf(), setOf()).find("Y9G-KS", "Yvaeroure")
        assertEquals(3, route.size)
        assertEquals(7, route[0].size)
        assertEquals(7, route[1].size)
        assertEquals(7, route[2].size)
        // routes are, but in random order:
        // Y9G-KS, Conomette, Yveve, Yvelet, Arasare, Vecodie, Yvaeroure
        // Y9G-KS, Pertnineere, Boystin, Oerse, Maire, Octanneve, Yvaeroure
        // Y9G-KS, Pertnineere, Boystin, Lour, Maire, Octanneve, Yvaeroure
    }
}
