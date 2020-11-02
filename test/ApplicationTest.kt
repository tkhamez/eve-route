package net.tkhamez.everoute

import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.config.HoconApplicationConfig
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI
import org.junit.BeforeClass

@KtorExperimentalAPI
class ApplicationTest {
    companion object {
        val engine = TestApplicationEngine(createTestEnvironment {
            config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
        })
        @BeforeClass @JvmStatic fun setup() {
            engine.start(wait = false)
            //engine.application.module()
        }
    }

    @Test
    fun testRoot() {
        //withTestApplication({ module(testing = true) }) {
        //withTestApplication({ module() }) {
        with(engine) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue(response.content?.contains("EVE Route Planner")!!); // "File not found." without frontend
            }
        }
    }
}
