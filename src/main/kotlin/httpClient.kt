package net.tkhamez.everoute

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.UserAgent
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging

val httpClient = HttpClient(Apache) {
    install(JsonFeature) {
        serializer = GsonSerializer()
    }
    install(Logging) {
        level = LogLevel.HEADERS
    }
    install(UserAgent) {
        agent = "eve-route/0.0.1 Ktor http-client"
    }
}
