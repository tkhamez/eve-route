package net.tkhamez.everoute

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.UserAgent
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import net.tkhamez.everoute.database.DbInterface
import net.tkhamez.everoute.database.Exposed
import net.tkhamez.everoute.database.Mongo
import java.util.*

val httpClient = HttpClient(Apache) {
    install(JsonFeature) {
        serializer = GsonSerializer()
    }
    install(Logging) {
        level = LogLevel.NONE
    }
    install(UserAgent) {
        agent = "EVE Route (https://github.com/tkhamez/eve-route) Ktor http-client"
    }
}

val gson: Gson = GsonBuilder().registerTypeAdapter(Date::class.java, GsonUTCDateAdapter()).create()

fun db(uri: String): DbInterface {
    return if (uri.startsWith("mongodb://") || uri.startsWith("mongodb+srv://")) {
        Mongo(uri)
    } else {
        Exposed(uri)
    }
}
