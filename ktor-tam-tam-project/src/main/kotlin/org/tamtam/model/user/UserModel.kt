package org.tamtam.model.user

import kotlinx.serialization.Serializable


@Serializable
data class UserRemoteModel(
    val id: String,
    var firstName: String,
    var lastName: String,
    var login: String,
    var password: String,
    var status : String,
    var image: String?,
    val contactsInformation: UserContactsInformationModel,
) {
    companion object {
        const val USER_STATUS_LOGGED_IN = "logged_in"
        const val USER_STATUS_LOGGED_OUT = "logged_out"
        const val USER_STATUS_DELETED = "deleted"
    }

    fun toUserResponse(userNotes: UserNotesModel?) : UserResponseModel {
        return UserResponseModel(
            id,
            firstName,
            lastName,
            login,
            image,
            contactsInformation,
            userNotes
        )
    }
}

@Serializable
data class UserContactsInformationModel(
    var phone: String?,
    var email: String?
)

@Serializable
data class UserNotesModel(
    val activeNotes: String,
    val pendingNotes: String
)


@Serializable
data class UserResponseModel(
    val id: String,
    val firstName: String,
    val lastName: String,
    val login: String,
    val image: String?,
    val contactsInformation: UserContactsInformationModel,
    val userNotes : UserNotesModel?
)

