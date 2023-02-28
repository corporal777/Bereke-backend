package org.tamtam

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.random.Random

fun Application.configureDatabase(){

}



fun generateToken(): String {
    val newToken = UUID.randomUUID().toString()
    return newToken.replace("-", "")
}

fun generateId() : String {
    return Random.nextInt(0, 90000).toString()
}

fun generateCode() : String {
    val random = java.util.Random()
    val number = random.nextInt(999999)
    return String.format("%06d", number);
}

fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        followRedirects = false
    }
}

fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val theta = lon1 - lon2
    var dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta))
    dist = Math.acos(dist)
    dist = rad2deg(dist)
    dist *= 60 * 1.1515
    dist *= 1.609344
    return dist
}

private fun deg2rad(deg: Double): Double {
    return deg * Math.PI / 180.0
}

private fun rad2deg(rad: Double): Double {
    return rad * 180.0 / Math.PI
}

fun isPhone(text: String?): Boolean {
    if (text.isNullOrEmpty()) return false
    val regex = Regex(pattern = "[0-9]+")
    return regex.containsMatchIn(text)
}