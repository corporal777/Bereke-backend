package org.tamtam.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.tamtam.db.NoteDataSource
import org.tamtam.db.StoriesDataSource
import org.tamtam.db.UserDataSource
import org.tamtam.routes.*
import java.io.FileInputStream


fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureFirebase() {
    val serviceAccount = FileInputStream("/Users/test/Documents/Projects/tam-tam-backend/ktor-tam-tam-project/src/main/resources/tam-tam-firebase-adminsdk.json")

    val options = FirebaseOptions.Builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setDatabaseUrl("https://tam-tam-8b2a7-default-rtdb.firebaseio.com")
        .build()

    FirebaseApp.initializeApp(options)
}

fun Application.configureUserRouting(userDataSource: UserDataSource) {
    routing {
        loginUser(userDataSource)
        logoutUser(userDataSource)
        registerUser(userDataSource)
        getUserById(userDataSource)
        checkEmailUnique(userDataSource)
        changePhoto(userDataSource)
        updateUser(userDataSource)
        getLocation(userDataSource)

        static("/") {
            staticBasePackage = "static"
            resource("russian-cities.json")
            this.get("file") {
                call.respond(this@static)
            }
        }
    }
}

fun Application.configureNoteRouting(noteDataSource: NoteDataSource) {
    routing {
        noteRoutes(noteDataSource)
        static("/") {
            staticBasePackage = "static"
            resource("russian-cities.json")
            this.get("file") {
                call.respond(this@static)
            }
        }
    }
}

fun Application.configureStoryRouting(storiesDataSource: StoriesDataSource) {
    routing {
        getStories(storiesDataSource)
    }
}