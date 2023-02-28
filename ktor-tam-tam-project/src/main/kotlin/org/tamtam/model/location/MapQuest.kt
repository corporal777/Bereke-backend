package org.tamtam.model.location

import kotlinx.serialization.Serializable

@Serializable
data class MapQuestRequestModel(
    val location: MapQuestLocationCoord,
    val options: MapQuestOptions,
    val includeNearestIntersection: Boolean,
    val includeRoadMetadata: Boolean
)

@Serializable
data class MapQuestOptions(
    val thumbMaps: Boolean,
)

@Serializable
data class MapQuestLocationCoord(
    val latLng: LatLng,
)

@Serializable
data class LatLng(
    val lat: Double,
    val lng: Double
)

@Serializable
data class MapQuestResponseModel(
    val results: List<MapQuestResult>
)

@Serializable
data class MapQuestResult(
    val locations: List<MapQuestLocation>
)

@Serializable
data class MapQuestLocation(
    val street: String,
    val adminArea6: String,
    val adminArea6Type: String,
    val adminArea5: String,
    val adminArea5Type: String,
    val adminArea4: String,
    val adminArea4Type: String,
    val adminArea3: String,
    val adminArea3Type: String,
    val adminArea1: String,
    val adminArea1Type: String,
)