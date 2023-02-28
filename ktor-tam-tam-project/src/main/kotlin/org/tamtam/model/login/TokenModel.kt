package org.tamtam.model.login

import kotlinx.serialization.Serializable

@Serializable
data class TokenRemoteModel(
    val deviceId : String,
    var createdAt : Long,
    var token : String
) {
    fun toTokenResponse() : TokenResponseModel {
        return TokenResponseModel(deviceId, token)
    }
}

@Serializable
data class TokenResponseModel(
    val deviceId : String,
    val token : String
)

@Serializable
data class FcmTokenModel(
    var deviceId: String,
    var userId: String?,
    var token: String
)