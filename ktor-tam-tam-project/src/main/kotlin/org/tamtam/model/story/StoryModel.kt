package org.tamtam.model.story

import kotlinx.serialization.Serializable

@Serializable
data class StoriesModel(
    val title : String,
    val logo : String,
    val stories : List<StoryModel>,
)

@Serializable
data class StoryModel(
    val title : String?,
    val message : String,
    val image : String,
    val phone : StoryPhoneModel,
    val date : String
)

@Serializable
data class StoryPhoneModel(
    val value: String,
    val type: String
)