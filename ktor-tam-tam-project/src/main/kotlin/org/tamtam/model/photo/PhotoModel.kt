package org.tamtam.model.photo

import kotlinx.serialization.Serializable
import org.bson.types.Binary

data class UserPhotoModel(
    val id : String,
    val image : Binary
)

@Serializable
data class FirebasePhotoResponseModel(
    val name : String,
    val bucket : String,
    val downloadTokens : String,
)

@Serializable
data class PhotoResponseModel(
    val id : String,
    val image : String,
)