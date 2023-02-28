package org.tamtam.db

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.tamtam.generateToken
import org.tamtam.model.exception.ExceptionResponse
import org.tamtam.model.login.FcmTokenModel
import org.tamtam.model.login.LoginRemoteModel
import org.tamtam.model.login.TokenRemoteModel
import org.tamtam.model.login.TokenResponseModel
import org.tamtam.model.notes.NoteResponseModel
import org.tamtam.model.photo.PhotoResponseModel
import org.tamtam.model.photo.UserPhotoModel
import org.tamtam.model.user.*
import org.tamtam.model.user.UserRemoteModel.Companion.USER_STATUS_LOGGED_IN
import org.tamtam.model.user.UserRemoteModel.Companion.USER_STATUS_LOGGED_OUT

class MongoUserDataSource(db: CoroutineDatabase) : UserDataSource {

    private val users = db.getCollection<UserRemoteModel>("users_collection")
    private val tokens = db.getCollection<TokenRemoteModel>("tokens_collection")
    private val fcmTokens = db.getCollection<FcmTokenModel>("fcm_tokens_collection")
    private val notesCollection = db.getCollection<NoteResponseModel>("notes_collection")


    override suspend fun loginUser(login: LoginRemoteModel): UserResponseModel? {
        val existsUser = users.findOne(UserRemoteModel::login eq login.login, UserRemoteModel::password eq login.password)

        if (existsUser != null) {
            existsUser.status = USER_STATUS_LOGGED_IN
            users.updateOne(UserRemoteModel::id eq existsUser.id, existsUser)
            return existsUser.toUserResponse(getUserNotes(existsUser.id))
        } else return null
    }

    override suspend fun logoutUser(id: String): Boolean {
        var isLoggedOut = true
        val existsUser = users.findOne(UserRemoteModel::id eq id)

        if (existsUser != null){
            existsUser.status = USER_STATUS_LOGGED_OUT
            isLoggedOut = users.updateOne(UserRemoteModel::id eq existsUser.id, existsUser).wasAcknowledged()
        }
        return isLoggedOut
    }

    override suspend fun insertUser(id: String, name: String, lastName: String, login: String, password: String): UserResponseModel? {
        val newUser = UserRemoteModel(
            id = id,
            firstName = name,
            lastName = lastName,
            login = login,
            password = password,
            image = null,
            status = USER_STATUS_LOGGED_IN,
            contactsInformation = UserContactsInformationModel(null, null)
        )
        val wasAcknowledged = users.insertOne(newUser).wasAcknowledged()
        return if (wasAcknowledged){
            newUser.toUserResponse(getUserNotes(newUser.id))
        }else null
    }

    override suspend fun getUserById(id: String): UserResponseModel? {
        val user = users.findOne(UserRemoteModel::id eq id)
        return user?.toUserResponse(getUserNotes(user.id))
    }

    override suspend fun updateUser(
        id: String,
        name: String?,
        lastName: String?,
        phone: String?,
        email: String?
    ): UserResponseModel? {

        val user = users.findOne(UserRemoteModel::id eq id)
        var responseUser: UserResponseModel? = null

        if (user != null) {
            if (!name.isNullOrEmpty() && name != user.firstName)
                user.firstName = name
            if (!lastName.isNullOrEmpty() && lastName != user.lastName)
                user.lastName = lastName
            if (!phone.isNullOrEmpty() && user.contactsInformation.phone != phone)
                user.contactsInformation.phone = phone
            if (!email.isNullOrEmpty() && user.contactsInformation.email != email)
                user.contactsInformation.email = email

            if (users.updateOne(UserRemoteModel::id eq user.id, user).wasAcknowledged()) {
                responseUser = user.toUserResponse(getUserNotes(user.id))
            }
        }
        return responseUser

    }

    override suspend fun createFcmToken(deviceId: String, userId: String, token: String): Boolean {
        val fcmToken = fcmTokens.findOne(FcmTokenModel::deviceId eq deviceId)

        if (fcmToken == null) {
            return fcmTokens.insertOne(FcmTokenModel(deviceId, userId, token)).wasAcknowledged()
        } else {
            if (fcmToken.token != token){
                fcmToken.token = token
                return fcmTokens.updateOne(FcmTokenModel::deviceId eq deviceId, fcmToken).wasAcknowledged()
            } else if (fcmToken.userId != userId){
                fcmToken.userId = userId
                return fcmTokens.updateOne(FcmTokenModel::deviceId eq deviceId, fcmToken).wasAcknowledged()
            }
            else return true
        }
    }



    override suspend fun checkUserEmailExists(login: String): Boolean {
        val existsUser = users.findOne(UserRemoteModel::login eq login)
        return existsUser != null
    }

    override suspend fun checkUserPasswordCorrect(userId: String, password: String): Boolean {
        var isCorrect = false
        val existsUser = users.findOne(UserRemoteModel::id eq userId)
        if (existsUser != null) {
            isCorrect = password == existsUser.password
        } else isCorrect = false

        return isCorrect
    }

    override suspend fun changeUserPassword(userId: String, password: String): Boolean {
        var isSuccess = false
        val existsUser = users.findOne(UserRemoteModel::id eq userId)
        if (existsUser != null) {
            existsUser.password = password
            isSuccess = users.updateOne(UserRemoteModel::id eq userId, existsUser).wasAcknowledged()
        } else isSuccess = false

        return isSuccess
    }

    override suspend fun changeUserLogin(userId: String, login: String): Boolean {
        var isSuccess = false
        val existsUser = users.findOne(UserRemoteModel::id eq userId)
        if (existsUser != null) {
            existsUser.login = login
            isSuccess = users.updateOne(UserRemoteModel::id eq userId, existsUser).wasAcknowledged()
        } else isSuccess = false

        return isSuccess
    }

    override suspend fun insertToken(deviceId: String): TokenResponseModel {
        var token = tokens.findOne(TokenRemoteModel::deviceId eq deviceId)
        if (token == null) {
            token = TokenRemoteModel(deviceId, System.currentTimeMillis(), generateToken())
            tokens.insertOne(token)
        } else {
            val today = System.currentTimeMillis()
            val tokenDate = ((today - token.createdAt) / (1000 * 60 * 60 * 24)).toInt()

            if (tokenDate > 29){
                token.createdAt = today
                token.token = generateToken()
                tokens.updateOne(TokenRemoteModel::deviceId eq deviceId, token)
            }
        }

        return token.toTokenResponse()
    }


    override suspend fun changePhoto(id: String, image: String): PhotoResponseModel? {
//        val binary = Binary(BsonBinarySubType.BINARY, file.readBytes())
//        val photo = UserPhotoModel(id, binary)
//        images.insertOne(photo)
//        return photo

        val existsUser = users.findOne(UserRemoteModel::id eq id)
        var photoResponseModel: PhotoResponseModel? = null
        if (existsUser != null) {
            existsUser.image = image
            if (users.updateOne(UserRemoteModel::id eq existsUser.id, existsUser).wasAcknowledged()) {
                photoResponseModel = PhotoResponseModel(id, image)
            }
        }
        return photoResponseModel
    }

    private suspend fun getUserNotes(userId: String): UserNotesModel {
        val userNotes = notesCollection.find(NoteResponseModel::createdBy eq userId).toList()
        val activeNotes = userNotes.filter { x -> x.status == "approved" }
        val pendingNotes = userNotes.filter { x -> x.status == "pending" }
        return UserNotesModel(activeNotes.size.toString(), pendingNotes.size.toString())
    }

    override suspend fun checkToken(call: ApplicationCall): Boolean {
        var hasToken = true
        val authHeader = call.request.header("Authorization")
        if (authHeader.isNullOrEmpty() || authHeader.length < 15) {
            hasToken = false
        } else {
            val str = StringBuilder(authHeader.replace(" ", ""))
            val authToken = str.replace(0, 5, "").toString()
            val remoteToken = tokens.findOne(TokenRemoteModel::token eq authToken)

            hasToken = remoteToken != null && remoteToken.token == authToken
        }

        if (!hasToken){
            call.respond(
                HttpStatusCode.Unauthorized,
                ExceptionResponse("Not correct Token!", HttpStatusCode.Unauthorized.value)
            )
        }
        return hasToken
    }
}