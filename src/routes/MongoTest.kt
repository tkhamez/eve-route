package net.tkhamez.everoute.routes

import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import org.litote.kmongo.*

fun Route.mongo(uri: String) {
    get("/mongo") {
        data class Jedi(val name: String, val age: Int)

        val dbName = uri.substring(uri.lastIndexOf('/') + 1)

        val client = KMongo.createClient(uri) // get com.mongodb.MongoClient new instance
        val database = client.getDatabase(dbName) // normal java driver usage
        val col = database.getCollection<Jedi>() // KMongo extension method
        // here the name of the collection by convention is "jedi"
        // you can use getCollection<Jedi>("other-jedi") if the collection name is different

        col.insertOne(Jedi("Luke Skywalker", 19))

        var yoda : Jedi? = col.findOne(Jedi::name eq "Yoda")
        if (yoda == null) {
            yoda = Jedi("Yoda", 900)
            col.insertOne(yoda)
        }

        // type-safe query style is recommended for complex apps ->
        val luke = col.aggregate<Jedi>(
                match(Jedi::age lt yoda.age),
                sample(1)
        ).first()

        if (luke != null) {
            call.respondText(luke.toString())
        }
    }
}
