package org.tamtam.model.notes

import kotlinx.serialization.Serializable

@Serializable
data class NoteLikeBody(
    val noteId: String,
    val userId: String
)