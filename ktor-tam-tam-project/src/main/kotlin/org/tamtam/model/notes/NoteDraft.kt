package org.tamtam.model.notes

import kotlinx.serialization.Serializable

@Serializable
data class NoteDraftRemoteModel(
    val user: String,
    var draft: NoteDraftModel
)

@Serializable
data class NoteDraftModel(
    val name: String,
    val description: String?,
    val salary: String?,
    val category: String,
    val contacts: NoteContactsModel,
    val address: NoteAddressModel,
    val additionalData: String
)