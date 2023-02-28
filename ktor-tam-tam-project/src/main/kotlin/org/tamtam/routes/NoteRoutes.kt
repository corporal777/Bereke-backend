package org.tamtam.routes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.litote.kmongo.json
import org.tamtam.cache.EmailUtil
import org.tamtam.cache.InMemoryCache
import org.tamtam.cache.LocationCache
import org.tamtam.cache.NotesCache
import org.tamtam.createHttpClient
import org.tamtam.db.NoteDataSource
import org.tamtam.generateId
import org.tamtam.generateToken
import org.tamtam.model.exception.ExceptionResponse
import org.tamtam.model.notes.*
import org.tamtam.model.photo.FirebasePhotoResponseModel

fun Route.noteRoutes(noteDataSource: NoteDataSource) {
    get("v1/notes-list") {
        if (!noteDataSource.checkToken(call)) return@get

        val params = call.request.queryParameters
        val list = noteDataSource.getNotesList(params)
        call.respond(list)
    }

    post("v1/create-note/{id}") {
        if (!noteDataSource.checkToken(call)) return@post

        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            val request = call.receive<NoteRequestModel>()
            val noteResponse = noteDataSource.createNote(id, request)
            if (noteResponse != null) {
                call.respond(noteResponse)
                noteDataSource.changeNoteStatus(noteResponse.id)
            } else call.respond(HttpStatusCode.Conflict)

        }
    }

    post("v1/create-note-data/{id}") {
        if (!noteDataSource.checkToken(call)) return@post

        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            val request = call.receive<Map<String, String>>()
            val additionalData = noteDataSource.createNoteAdditionalData(id, request)
            if (additionalData != null) {
                call.respond(additionalData)
            } else call.respond(HttpStatusCode.Conflict)
        }
    }

    get("v1/note-detail/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            val noteDetail = noteDataSource.getNoteDetail(id)
            //val noteDetail = NotesCache.getNoteDetail()
            if (noteDetail != null) call.respond(noteDetail)
            else call.respond(
                HttpStatusCode.NotFound,
                ExceptionResponse("Note not found", HttpStatusCode.NotFound.value)
            )


//            val noteDetail = NotesCache.getNoteDetail(id)
//            if (noteDetail != null)
//                call.respond(noteDetail)
        }


//        val note = InMemoryCache.getTestNote()
//        val additionalWorkData = InMemoryCache.getTestNoteAdditionalData()
//        val additionalNoteData = InMemoryCache.getHomeNoteAdditionalData()
////        val json = Json.encodeToString(additionalData)
////        val jsonString = Gson().toJson(additionalData)
//        val obj = Json.encodeToJsonElement(additionalWorkData)
//        call.respond(NoteDetailModel(note, obj))
//
//        val opt: WorkNoteAdditionalData = Json.decodeFromJsonElement(obj)
    }


    post("v1/change-note-images/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        } else {
            val client = createHttpClient()
            val imagesList = arrayListOf<String>()
            call.receiveMultipart().forEachPart { part ->
                if (part is PartData.FileItem) {
                    call.application.environment.log.error(part.originalFileName)
                    val fileBytes = part.streamProvider().readBytes()
                    imagesList.add(loadNoteImagesToFirebase(client, fileBytes, id))
                }
                part.dispose()
            }
            client.close()

            val updatedNote = noteDataSource.updateNoteImages(id, imagesList)
            if (updatedNote != null) {
                call.respond(updatedNote)
            } else call.respond(
                HttpStatusCode.NotFound,
                ExceptionResponse("Note not found", HttpStatusCode.NotFound.value)
            )
        }
    }

    post("v1/create-note-draft/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            val request = call.receive<NoteDraftModel>()
            val createdDraft = noteDataSource.createNoteDraft(id, request)
            call.respond(createdDraft)
        }
    }

    get("v1/get-note-draft/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            val draft = noteDataSource.getNoteDraft(id)
            if (draft == null) {
                call.respond(HttpStatusCode.NotFound)
            } else call.respond(draft)
        }
    }

    patch("v1/add-note-favorite") {
        val request = call.receive<NoteLikeBody>()
        val note = noteDataSource.addNoteToFavorite(request.noteId, request.userId)
        if (note != null) call.respond(note)
        else call.respond(HttpStatusCode.BadRequest)
    }

    patch("v1/delete-note-favorite") {
        val request = call.receive<NoteLikeBody>()
        val note = noteDataSource.removeNoteFromFavorite(request.noteId, request.userId)
        if (note != null) call.respond(note)
        else call.respond(HttpStatusCode.BadRequest)
    }


}

suspend fun loadNoteImagesToFirebase(client: HttpClient, bytes: ByteArray, noteId: String): String {
    val fileName = "(" + noteId + ")" + generateToken().substring(0, 10)
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
    return imageUrl
}

private fun getFirebaseImageUrl(fileName: String): String {
    return "https://firebasestorage.googleapis.com/v0/b/tam-tam-8b2a7.appspot.com/o/notes_images%2F$fileName"
}