package org.tamtam.db

import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.json.JsonElement
import org.tamtam.model.notes.*
import org.tamtam.model.user.UserResponseModel

interface NoteDataSource {
    suspend fun createNote(userId : String, note: NoteRequestModel): NoteResponseModel?
    suspend fun createNoteAdditionalData(noteId : String, map: Map<String, String>): JsonElement?
    suspend fun updateNoteImages(noteId : String, images: List<String>): NoteResponseModel?

    suspend fun getNotesList(params : Parameters) : List<NoteResponseModel>


    suspend fun getNoteDetail(noteId : String) : NoteDetailModel?

    suspend fun activateNote(noteId : String, userId: String): NoteResponseModel?
    suspend fun deActivateNote(noteId : String, userId: String): NoteResponseModel?

    suspend fun changeNoteStatus(noteId: String)

    suspend fun checkToken(call: ApplicationCall): Boolean

    suspend fun createNoteDraft(userId: String, draft : NoteDraftModel) : NoteDraftRemoteModel
    suspend fun getNoteDraft(userId: String) : NoteDraftRemoteModel?

    suspend fun addNoteToFavorite(noteId: String, userId: String) : NoteResponseModel?
    suspend fun removeNoteFromFavorite(noteId: String, userId: String) : NoteResponseModel?
}