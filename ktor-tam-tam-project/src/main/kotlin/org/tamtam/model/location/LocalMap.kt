package org.tamtam.model.location

import kotlinx.serialization.Serializable

@Serializable
data class LocalCityModel(
    val coords: LocalCityCoordinates,
    val district: String?,
    val name: String,
    val population: Int?,
    val subject : String
)

@Serializable
data class LocalCityCoordinates(
    val lat: String,
    val lon: String
)

@Serializable
data class LocalCitiesModel(
    val data: List<LocalCityModel>
)