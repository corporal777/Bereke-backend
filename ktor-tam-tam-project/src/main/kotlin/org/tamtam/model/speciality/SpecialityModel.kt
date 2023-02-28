package org.tamtam.model.speciality

import kotlinx.serialization.Serializable


@Serializable
data class SpecialityModel(
    val id: String,
    val name: String,
    val specializations : List<Specialization>
)

@Serializable
data class Specialization(
    val id: String,
    val name: String,
    val laboring: Boolean
)

@Serializable
data class SpecialityResponse(
    val id : String,
    val catalogue : String,
    val name : String
)