package org.tamtam.db

import io.ktor.server.application.*
import org.tamtam.model.login.LoginRemoteModel
import org.tamtam.model.login.TokenRemoteModel
import org.tamtam.model.login.TokenResponseModel
import org.tamtam.model.photo.PhotoResponseModel
import org.tamtam.model.user.UserResponseModel

interface UserDataSource {

    suspend fun loginUser(login: LoginRemoteModel): UserResponseModel?
    suspend fun logoutUser(id: String): Boolean

    suspend fun insertUser(
        id: String,
        name: String,
        lastName: String,
        login: String,
        password: String
    ): UserResponseModel?

    suspend fun getUserById(id: String): UserResponseModel?


    suspend fun updateUser(
        id: String,
        name: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): UserResponseModel?

    suspend fun createFcmToken(deviceId: String, userId: String, token: String): Boolean

    suspend fun checkUserEmailExists(login: String): Boolean
    suspend fun checkUserPasswordCorrect(userId: String, password: String): Boolean
    suspend fun changeUserPassword(userId: String, password: String): Boolean
    suspend fun changeUserLogin(userId: String, login: String): Boolean

    suspend fun insertToken(deviceId : String): TokenResponseModel


    suspend fun changePhoto(id: String, image: String): PhotoResponseModel?

    suspend fun checkToken(call: ApplicationCall): Boolean

}