package org.tamtam.model.location

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DataArea(
    val id: String,
    val parent_id: String?,
    val name: String,
    val areas: List<DataArea>
)

@Serializable
data class MetroStationsData(
    val id: String,
    val name: String,
    val lines: List<LinesModel>
)

@Serializable
data class LinesModel(
    val id: String,
    val hex_color: String,
    val name: String,
    val stations: List<StationModel>
)

@Serializable
data class StationModel(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
)

@Serializable
data class MetroStationsResponseModel(
    val name: String,
    val color: ArrayList<String>,
    val lat: String,
    val lng: String
)