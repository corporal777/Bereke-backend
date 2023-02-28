package org.tamtam.routes

import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.tamtam.createHttpClient
import org.tamtam.db.UserDataSource
import org.tamtam.model.exception.ExceptionResponse
import org.tamtam.model.photo.FirebasePhotoResponseModel


fun Route.updateUser(userDataSource: UserDataSource) {
    post("v1/update-user/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        } else {
            val request = call.receive<Map<String, String>>()

            val name = request["firstName"]
            val lastName = request["lastName"]
            val email = request["email"]
            val phone = request["phone"]

            val user = userDataSource.updateUser(id, name, lastName, phone, email)
            if (user != null) {
                call.respond(user)
            } else call.respond(HttpStatusCode.BadRequest)
        }
    }
}

fun Route.getUserById(userDataSource: UserDataSource) {
    get("v1/user/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }
        val user = userDataSource.getUserById(id)
        if (user == null) {
            call.respond(
                HttpStatusCode.NotFound,
                ExceptionResponse("User not found!", HttpStatusCode.NotFound.value)
            )
        } else {
            call.respond(user)
        }
    }
}

fun Route.checkEmailUnique(userDataSource: UserDataSource) {
    get("v1/check-login/{login}") {
        val email = call.parameters["login"]
        if (email.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }
        if (userDataSource.checkUserEmailExists(email)) {
            call.respond(
                HttpStatusCode.Conflict,
                ExceptionResponse("Login is not free!", HttpStatusCode.Conflict.value)
            )
        } else call.respond(HttpStatusCode.OK)
    }

    get("v1/check-password/{id}") {
        val id = call.parameters["id"]
        val password = call.request.queryParameters["password"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }else {
            if (userDataSource.checkUserPasswordCorrect(id, password?:"")) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    ExceptionResponse("User password is not correct!", HttpStatusCode.NotFound.value)
                )
            }
        }

    }
    patch ("v1/change-password/{id}") {
        val id = call.parameters["id"]

        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@patch
        }else {
            val request = call.receive<Map<String, String>>()
            val password = request["password"]

            if (userDataSource.changeUserPassword(id, password?:"")) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    ExceptionResponse("User not found or not updated!", HttpStatusCode.NotFound.value)
                )
            }
        }
    }

    patch("v1/change-login/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@patch
        }else {
            val request = call.receive<Map<String, String>>()
            val login = request["login"]

            if (userDataSource.changeUserLogin(id, login?:"")) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    ExceptionResponse("User not found or not updated!", HttpStatusCode.NotFound.value)
                )
            }
        }
    }
}

fun Route.changePhoto(userDataSource: UserDataSource) {
    post("v1/change-user-photo/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.Conflict)
            return@post
        } else {
            var imageUrlToSave = ""
            val multipartData = call.receiveMultipart()
            multipartData.forEachPart { part ->
                if (part is PartData.FileItem) {
                    call.application.environment.log.error(part.originalFileName)
                    val fileBytes = part.streamProvider().readBytes()
                    imageUrlToSave = loadImageToFirebase(id, fileBytes)
                }
                part.dispose()
            }
            if (imageUrlToSave.isNullOrEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Image URL is null!")
                return@post
            } else {
                val photoModel = userDataSource.changePhoto(id, imageUrlToSave)
                if (photoModel != null) {
                    call.respond(photoModel)
                } else call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

suspend fun loadImageToFirebase(id: String, bytes: ByteArray): String {
//    upload to mongo db
//    val file = File(part.name ?: "image").apply {
//        writeBytes(fileBytes)
//    }
//    val photo = userDataSource.changePhoto(id, file)
//    val encoded = Base64.getEncoder().encodeToString(photo.image.data)
//    call.respondText(encoded)
    val client = createHttpClient()
    val fileName = "user_image$id"
    val url = getFirebaseImageUrl(fileName)
    val response: HttpResponse = client.submitFormWithBinaryData(
        url = url,
        formData = formData {
            append("image", bytes, Headers.build {
                append(HttpHeaders.ContentType, "image/png")
                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
            })
        }
    )
    var imageUrl = ""
    if (response.status == HttpStatusCode.OK) {
        val json: FirebasePhotoResponseModel = response.body()
        if (json != null) {
            val token = json.downloadTokens
            imageUrl = URLBuilder(url).apply {
                set {
                    parameters.append("alt", "media")
                    parameters.append("token", token)
                }
            }.buildString()
        }
    }
    client.close()
    return imageUrl
}

private fun getFirebaseImageUrl(fileName: String): String {
    return "https://firebasestorage.googleapis.com/v0/b/tam-tam-8b2a7.appspot.com/o/a_test%2F$fileName"
}

