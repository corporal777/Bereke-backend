package org.tamtam.model.login

import kotlinx.serialization.Serializable

@Serializable
data class LoginRemoteModel(
    val login: String,
    val password : String
)

@Serializable
data class RegisterLoginModel(
    val login: String,
    val loginType : String
)

@Serializable
data class VerifyLoginModel(
    val login: String,
    val code : String
)