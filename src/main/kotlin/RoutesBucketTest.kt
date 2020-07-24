package net.tkhamez.everoute

import com.google.cloud.firestore.FirestoreOptions
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.bucketTest() {
    get("/bucket-test") {

        val args: List<String> = listOf(
                "test" // YOUR_COLLECTION_NAME
                //,"ff0" // KEY
                //,"vv2" // VALUE
        )

        // validate the arguments
        if (args.isEmpty() || args.size > 3) {
            throw Exception("Usage: java -jar firestore.jar YOUR_COLLECTION_ID [KEY] [VALUE]")
        }

        // create the client
        val db = FirestoreOptions.newBuilder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
                .service

        // create the docRef and data object
        val docRef = db.collection(args[0]).document("samples")
        var data = docRef
                .get() // future
                .get() // snapshot
                .data // MutableMap

        // initialize document with empty data object if it doesn't exist
        if (data == null) {
            data = mutableMapOf<String, Any>()
            docRef.set(data)
        }

        // If no arguments are supplied, call the quickstart. Fetch the key value if only one argument is supplied.
        // Set the key to the supplied value if two arguments are supplied.
        val output = when (args.size) {
            1 -> quickstart(args[0], "samples")
            2 -> "${args[1]}: ${data[args[1]] ?: "not found"}"
            else -> {
                val future = docRef.update(args[1], args[2])
                "Updated collection: ${future.get()}"
            }
        }

        call.respondText("bucket test:<br><br> $output", contentType = ContentType.Text.Html)
    }
}

fun quickstart(collectionName: String, documentName: String): String {
    // [START firestore_quickstart]
    // Create the client.
    val db = FirestoreOptions.newBuilder()
            .setTimestampsInSnapshotsEnabled(true)
            .build()
            .service

    // Fetch the document reference and data object.
    val docRef = db.collection(collectionName).document(documentName)
    val data = docRef
            .get() // future
            .get() // snapshot
            .data ?: error("Document $collectionName:$documentName not found") // MutableMap

    // Print the retrieved data.
    var output = ""
    data.forEach { (key, value) ->
        output = "$key: $value"
    }
    // [END firestore_quickstart]

    return output
}
