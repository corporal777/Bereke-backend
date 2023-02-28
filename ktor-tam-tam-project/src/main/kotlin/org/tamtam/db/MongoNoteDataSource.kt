package org.tamtam.db

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.CoroutineFindPublisher
import org.tamtam.cache.InMemoryCache
import org.tamtam.generateId
import org.tamtam.isPhone
import org.tamtam.model.exception.ExceptionResponse
import org.tamtam.model.login.FcmTokenModel
import org.tamtam.model.login.TokenRemoteModel
import org.tamtam.model.notes.*
import org.tamtam.model.notes.NoteResponseModel.Companion.STATUS_APPROVED
import org.tamtam.model.notes.NoteResponseModel.Companion.STATUS_INACTIVE
import org.tamtam.model.notes.NoteResponseModel.Companion.STATUS_PENDING
import org.tamtam.model.user.UserNotesModel
import org.tamtam.model.user.UserRemoteModel
import org.tamtam.model.user.UserRemoteModel.Companion.USER_STATUS_LOGGED_IN
import org.tamtam.model.user.UserResponseModel
import java.util.*
import kotlin.collections.ArrayList


class MongoNoteDataSource(db: CoroutineDatabase) : NoteDataSource {


    private val tokens = db.getCollection<TokenRemoteModel>("tokens_collection")
    private val users = db.getCollection<UserRemoteModel>("users_collection")
    private val fcmTokens = db.getCollection<FcmTokenModel>("fcm_tokens_collection")
    private val notesCollection = db.getCollection<NoteResponseModel>("notes_collection")

    private val workNotesData = db.getCollection<WorkNoteAdditionalData>("work_notes_additional_data")
    private val houseNotesData = db.getCollection<HouseNoteAdditionalData>("house_notes_additional_data")

    private val draftNotesData = db.getCollection<NoteDraftRemoteModel>("notes_draft_data")


    override suspend fun getNoteDetail(noteId: String): NoteDetailModel? {
        val note = notesCollection.findOne(NoteResponseModel::id eq noteId)
        var json: JsonElement? = null
        var userResponse: UserResponseModel? = null

        if (note != null) {
            val user = users.findOne(UserRemoteModel::id eq note.createdBy)
            if (user != null) {
                userResponse = user.toUserResponse(getUserNotes(user.id))
            }

            when (note.category) {
                NoteResponseModel.WORK_CATEGORY -> {
                    val workData = workNotesData.findOne(WorkNoteAdditionalData::noteId eq noteId)
                    json = Json.encodeToJsonElement(workData)
                }

                NoteResponseModel.HOUSE_CATEGORY -> {
                    val houseData = houseNotesData.findOne(HouseNoteAdditionalData::noteId eq noteId)
                    json = Json.encodeToJsonElement(houseData)
                }
            }
            return NoteDetailModel(note, userResponse, json)
        } else return null
    }

    override suspend fun getNotesList(params: Parameters): List<NoteResponseModel> {
        val userId = params["userId"]
        val status = params["status"]
        val limit = params["limit"]
        val offset = params["offset"]
        val favorite = params["favorite"]

        val subCategory = params["subCategory"]

        val workSpeciality = params["workSpeciality"]
        val houseType = params["houseType"]

        val listFilters = and(arrayListOf<Bson>().apply {
            if (!userId.isNullOrBlank()) add(NoteResponseModel::createdBy eq userId)
            if (!status.isNullOrBlank()) add(NoteResponseModel::status eq status)
            if (!favorite.isNullOrBlank()) add(NoteResponseModel::likes contains favorite)

            //filter by subCategory
            if (!subCategory.isNullOrEmpty()) {
                val noteIds = arrayListOf<String>()
                val sub = subCategory.split(",")

                val workData = workNotesData.find(WorkNoteAdditionalData::subCategory `in` sub).toList()
                workData.forEach { noteIds.add(it.noteId) }
                val houseData = houseNotesData.find(HouseNoteAdditionalData::subCategory `in` sub).toList()
                houseData.forEach { noteIds.add(it.noteId) }
                //if (!noteIds.isNullOrEmpty()) add(NoteResponseModel::id `in` (noteIds))
                add(NoteResponseModel::id `in` (noteIds))
            }

            //filter by work speciality
            if (!workSpeciality.isNullOrEmpty()) {
                val noteIds = arrayListOf<String>()
                val workData = workNotesData.find(WorkNoteAdditionalData::workSpeciality `in` workSpeciality).toList()
                workData.forEach { noteIds.add(it.noteId) }
                add(NoteResponseModel::id `in` (noteIds))
            }
            //filter by house type
            if (!houseType.isNullOrEmpty()){
                val noteIds = arrayListOf<String>()
                val houseData = houseNotesData.find(HouseNoteAdditionalData::houseType `in` houseType).toList()
                houseData.forEach { noteIds.add(it.noteId) }
                //if (!noteIds.isNullOrEmpty()) add(NoteResponseModel::id `in` (noteIds))
                add(NoteResponseModel::id `in` (noteIds))
            }
        })


//        val filterUser = if (userId.isNullOrBlank()) NoteResponseModel::createdBy ne userId
//        else NoteResponseModel::createdBy eq userId
//
//        val filterStatus = if (status.isNullOrBlank()) NoteResponseModel::status ne status
//        else NoteResponseModel::status eq status
//
//        val filterFavorite = if (favorite.isNullOrBlank()) NoteResponseModel::likes.contains(favorite)
//        else NoteResponseModel::status eq status

        val notes = notesCollection
            .find(listFilters)
            .let {
                if (!offset.isNullOrEmpty()) it.skip(offset.toInt())
                if (!limit.isNullOrEmpty()) it.limit(limit.toInt())
                else it
            }
            .sortByDesc()
            .toList()


        println("SUB CATEGORY " + subCategory)
        println("SIZE " + notes.size)
        return notes
    }



    override suspend fun createNote(userId: String, note: NoteRequestModel): NoteResponseModel? {
        val noteResponse = NoteResponseModel(
            generateId(),
            note.name,
            note.description,
            formatSalary(note.salary),
            note.category,
            STATUS_PENDING,
            userId,
            System.currentTimeMillis().toString(),
            null,
            note.contacts,
            note.address
        )
        val acknowledged = notesCollection.insertOne(noteResponse).wasAcknowledged()
        return if (acknowledged) noteResponse
        else null
    }

    override suspend fun createNoteAdditionalData(noteId: String, map: Map<String, String>): JsonElement? {
        var json: JsonElement? = null
        val note = notesCollection.findOne(NoteResponseModel::id eq noteId)

        if (note != null) {
            when (note.category) {
                NoteResponseModel.WORK_CATEGORY -> {
                    val workData = WorkNoteAdditionalData(
                        noteId,
                        map.get("subCategory") ?: "",
                        map.get("workSchedule"),
                        map.get("workExperience"),
                        map.get("workSpeciality"),
                        map.get("workComment")
                    )
                    if (workNotesData.insertOne(workData).wasAcknowledged())
                        json = Json.encodeToJsonElement(workData)
                }

                NoteResponseModel.HOUSE_CATEGORY -> {
                    val houseData = HouseNoteAdditionalData(
                        noteId,
                        map.get("subCategory") ?: "",
                        map.get("houseType"),
                        map.get("houseTerm"),
                        map.get("houseRooms"),
                        map.get("houseComment"),
                    )
                    if (houseNotesData.insertOne(houseData).wasAcknowledged())
                        json = Json.encodeToJsonElement(houseData)
                }
            }

        }
        return json
    }

    override suspend fun updateNoteImages(noteId: String, images: List<String>): NoteResponseModel? {
        var acknowledged = false
        val noteModel = notesCollection.findOne(NoteResponseModel::id eq noteId)

        if (noteModel != null) {
            noteModel.images = images
            acknowledged = notesCollection.updateOne(NoteResponseModel::id eq noteModel.id, noteModel).wasAcknowledged()
        }

        return if (acknowledged) noteModel
        else null
    }

    override suspend fun createNoteDraft(userId: String, draft: NoteDraftModel): NoteDraftRemoteModel {
        val noteDraft = draftNotesData.findOne(NoteDraftRemoteModel::user eq userId)
        if (noteDraft == null) {
            val draftRemote = NoteDraftRemoteModel(userId, draft)
            draftNotesData.insertOne(draftRemote)
            return draftRemote
        } else {
            noteDraft.draft = draft
            draftNotesData.updateOne(NoteDraftRemoteModel::user eq userId, noteDraft)
            return noteDraft
        }
    }

    override suspend fun getNoteDraft(userId: String): NoteDraftRemoteModel? {
        return draftNotesData.findOne(NoteDraftRemoteModel::user eq userId)
    }

    override suspend fun addNoteToFavorite(noteId: String, userId: String): NoteResponseModel? {
        val note = notesCollection.findOne(NoteResponseModel::id eq noteId)
        var isSuccess = false
        if (note != null && note.createdBy != userId) {
            note.likes.add(userId)
            isSuccess = notesCollection.updateOne(NoteResponseModel::id eq noteId, note).wasAcknowledged()
        }
        if (isSuccess) return note
        else return null
    }

    override suspend fun removeNoteFromFavorite(noteId: String, userId: String): NoteResponseModel? {
        val note = notesCollection.findOne(NoteResponseModel::id eq noteId)
        var isSuccess = false
        if (note != null) {
            note.likes.remove(userId)
            isSuccess = notesCollection.updateOne(NoteResponseModel::id eq noteId, note).wasAcknowledged()
        }
        if (isSuccess) return note
        else return null
    }

    private fun formatSalary(salary: String?): String {
        return if (salary.isNullOrEmpty()) ""
        else {
            if (isPhone(salary) && salary.length == 5) {
                StringBuilder().apply {
                    salary.forEachIndexed { index, c ->
                        if (index == 1) append("$c ")
                        else append(c)
                    }
                }.toString()
            } else if (isPhone(salary) && salary.length == 6) {
                StringBuilder().apply {
                    salary.forEachIndexed { index, c ->
                        if (index == 2) append("$c ")
                        else append(c)
                    }
                }.toString()
            } else salary
        }
    }

    override suspend fun changeNoteStatus(noteId: String) {
        coroutineScope {
            delay(50000)
            val note = notesCollection.findOne(NoteResponseModel::id eq noteId)
            if (note != null) {
                note.status = STATUS_APPROVED
                val success = notesCollection.updateOne(NoteResponseModel::id eq note.id, note).wasAcknowledged()
                if (success) {
                    createNotification(note.createdBy, "Объявление опубликовано", note.name)
                }
            }
        }
    }

    override suspend fun activateNote(noteId: String, userId: String): NoteResponseModel? {
        val note = notesCollection.findOne(NoteResponseModel::id eq noteId)
        if (note != null && note.status != STATUS_PENDING && note.createdBy == userId){
            note.status = STATUS_APPROVED
            notesCollection.updateOne(NoteResponseModel::id eq note.id, note).wasAcknowledged()
            return note
        } else return null
    }

    override suspend fun deActivateNote(noteId: String, userId: String): NoteResponseModel? {
        val note = notesCollection.findOne(NoteResponseModel::id eq noteId)
        if (note != null && note.status != STATUS_PENDING && note.createdBy == userId){
            note.status = STATUS_INACTIVE
            notesCollection.updateOne(NoteResponseModel::id eq note.id, note).wasAcknowledged()
            return note
        } else return null
    }

    private fun <T : Any> CoroutineFindPublisher<T>.sortByDesc(): CoroutineFindPublisher<T> {
        return this.sort(Document("_id", -1))
    }


    private suspend fun createNotification(userId: String, title: String, message: String) {
        val user = users.findOne(UserRemoteModel::id eq userId)
        if (user != null && user.status == USER_STATUS_LOGGED_IN) {
            val fcmToken = fcmTokens.findOne(FcmTokenModel::userId eq userId)
            if (fcmToken != null && fcmToken.userId?.toInt() != -1) {
                InMemoryCache.sendSimpleNotification(fcmToken.token, title, message)
            }
        }
    }

    override suspend fun checkToken(call: ApplicationCall): Boolean {
        var hasToken = true
        val authHeader = call.request.header("Authorization")
        if (authHeader.isNullOrEmpty() || authHeader.length < 15) {
            hasToken = false
        } else {
            val str = StringBuilder(authHeader.replace(" ", ""))
            val authToken = str.replace(0, 5, "").toString()
            val remoteToken = tokens.findOne(TokenRemoteModel::token eq authToken)

            hasToken = remoteToken != null && remoteToken.token == authToken
        }

        if (!hasToken) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ExceptionResponse("Not correct Token!", HttpStatusCode.Unauthorized.value)
            )
        }
        return hasToken
    }

    private suspend fun getUserNotes(userId: String): UserNotesModel {
        val userNotes = notesCollection.find(NoteResponseModel::createdBy eq userId).toList()
        val activeNotes = userNotes.filter { x -> x.status == "approved" }
        val pendingNotes = userNotes.filter { x -> x.status == "pending" }
        return UserNotesModel(activeNotes.size.toString(), pendingNotes.size.toString())
    }

}