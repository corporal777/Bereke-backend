package org.tamtam.model.location

import kotlinx.serialization.Serializable


@Serializable
data class LocationRequestModel(
    val lat: Double,
    val lon: Double
)

@Serializable
data class LocationResponseModel(
    val coords: LocationRequestModel,
    val city: String?,
    val district: String?,
    val country: String?
)


