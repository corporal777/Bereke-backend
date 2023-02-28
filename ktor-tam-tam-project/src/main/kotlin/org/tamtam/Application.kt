package org.tamtam


import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.tamtam.cache.InMemoryCache
import org.tamtam.db.MongoNoteDataSource
import org.tamtam.db.MongoStoriesDataSource
import org.tamtam.db.MongoUserDataSource
import org.tamtam.plugins.*


fun main() {
//    Database.connect("jdbc:postgresql://rysbek:KMgEIsVcdNsGWH5qz0GQd9EDV3lbHG1b@dpg-cf2jkb9gp3jl0q21iafg-a.frankfurt-postgres.render.com:5432/postgres",
//        driver = "org.postgresql.Driver",
//        password = "KMgEIsVcdNsGWH5qz0GQd9EDV3lbHG1b")

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)

}

fun Application.module() {

    //val mongoPassword = System.getenv("MONGO_PASSWORD")
    val mongoPassword = InMemoryCache.mongoPassword
    val dbName = InMemoryCache.dbName
    val db = KMongo.createClient(
        connectionString = "mongodb+srv://tamtam:$mongoPassword@clusterone.cvpxsch.mongodb.net/$dbName?retryWrites=true&w=majority"
    )
        .coroutine
        .getDatabase(dbName)

    val userDataSource = MongoUserDataSource(db)
    val noteDataSource = MongoNoteDataSource(db)
    val storiesDataSource = MongoStoriesDataSource(db)

    configureFirebase()
    configureSerialization()
    configureUserRouting(userDataSource)
    configureNoteRouting(noteDataSource)
    configureStoryRouting(storiesDataSource)
}
