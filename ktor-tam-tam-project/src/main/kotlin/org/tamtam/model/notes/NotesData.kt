package org.tamtam.model.notes

import kotlinx.serialization.Serializable


@Serializable
data class NoteActivateBody(
    val noteId : String,
    val activateBy: String
)

@Serializable
data class NoteDeActivateBody(
    val noteId : String,
    val deActivateBy: String
)

@Serializable
data class WorkNoteAdditionalData(
    val noteId : String,
    val subCategory : String,
    val workSchedule : String?,
    val workExperience : String?,
    val workSpeciality : String?,
    val workComment : String?
)

@Serializable
data class HouseNoteAdditionalData(
    val noteId : String,
    val subCategory : String,
    val houseType : String?,
    val houseTerm : String?,
    val houseRoomCount : String?,
    val houseComment : String?
)