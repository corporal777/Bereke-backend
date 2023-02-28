package org.tamtam.model.notes

import kotlinx.serialization.*
import kotlinx.serialization.json.JsonElement
import org.tamtam.model.user.UserResponseModel


@Serializable
data class NoteResponseModel(
    val id: String,
    val name: String,
    val description: String?,
    val salary: String?,
    val category : String,
    var status : String,
    val createdBy: String,
    val createdAt: String,
    var images: List<String>?,
    val contacts: NoteContactsModel,
    val address: NoteAddressModel,
    var likes: ArrayList<String> = arrayListOf()
) {
    companion object {
        const val WORK_CATEGORY = "work"
        const val SERVICE_CATEGORY = "service"
        const val HOUSE_CATEGORY = "house"
        const val PURCHASE_SALE_CATEGORY = "purchaseAndSale"

        const val STATUS_APPROVED = "approved"
        const val STATUS_PENDING = "pending"
        const val STATUS_INACTIVE = "inactive"
    }
}

@Serializable
data class NoteRequestModel(
    val name: String,
    val description: String?,
    val salary: String?,
    val category : String,
    val images: List<String>?,
    val contacts: NoteContactsModel,
    val address: NoteAddressModel,
)


@Serializable
data class NoteContactsModel(
    val phone: List<String>?,
    val whatsapp: List<String>?,
    val email: List<String>?
)

@Serializable
data class NoteLikesModel(
    var likes: List<String> = emptyList(),
    var viewings: List<String> = emptyList()
)

@Serializable
data class NoteAddressModel(
    val region: String?,
    val city: String,
    val street: String?,
    val house: String?,
    val metro: List<String>?,
    var lat: String?,
    var lon: String?
)

@Serializable
data class NoteDetailModel(
    val note : NoteResponseModel,
    val creator : UserResponseModel?,
    val additionalData : JsonElement?
)

