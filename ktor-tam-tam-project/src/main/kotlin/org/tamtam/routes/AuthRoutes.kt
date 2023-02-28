package org.tamtam.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.tamtam.cache.EmailUtil
import org.tamtam.db.UserDataSource
import org.tamtam.generateCode
import org.tamtam.generateId
import org.tamtam.generateToken
import org.tamtam.model.exception.ExceptionResponse
import org.tamtam.model.login.LoginRemoteModel
import org.tamtam.model.login.RegisterLoginModel
import org.tamtam.model.login.TokenRemoteModel
import org.tamtam.model.login.VerifyLoginModel

fun Route.loginUser(userDataSource: UserDataSource) {

    post("v1/send-verify-code") {
        val request = call.receive<RegisterLoginModel>()
        val code = generateCode()
        EmailUtil.addCodeAndEmail(request.login, code)

        if (request.loginType == "email") {
            val successSentCode = EmailUtil.sendEmailNew(request.login, code)
            if (successSentCode) {
                call.respond(HttpStatusCode.OK)
            } else call.respond(HttpStatusCode.BadRequest)
        }
    }


    post("v1/check-verify-code") {
        val request = call.receive<VerifyLoginModel>()
        val code = EmailUtil.getCode(request.login)
        if (code == request.code) {
            call.respond(HttpStatusCode.OK)
        } else call.respond(
            HttpStatusCode.NotFound,
            ExceptionResponse("Verify code is not correct!", HttpStatusCode.NotFound.value)
        )
    }

    post("v1/save-fcm-token") {
        val request = call.receive<Map<String, String>>()
        val userId = request["userId"]?:""
        val token = request["token"]!!
        val deviceId = request["deviceId"]!!
        if (userDataSource.createFcmToken(deviceId, userId, token)) {
            call.respond(HttpStatusCode.OK)
            return@post
        } else call.respond(HttpStatusCode.BadRequest)
    }

    post("v1/login") {
        val request = call.receiveNullable<LoginRemoteModel>() ?: kotlin.run {
            call.respond(HttpStatusCode.BadRequest, "Body cannot be null!")
            return@post
        }
        val user = userDataSource.loginUser(request)
        if (user != null) {
            call.respond(user)
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ExceptionResponse("Login or password is not correct!", HttpStatusCode.NotFound.value)
            )
            return@post
        }
    }
}


fun Route.logoutUser(userDataSource: UserDataSource) {
    post("v1/logout/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "Path cannot be null!")
            return@post
        }
        val isLoggedOut = userDataSource.logoutUser(id)
        if (isLoggedOut) {
            call.respond(HttpStatusCode.OK, "User is logged out!")
        } else call.respond(HttpStatusCode.Conflict, "User is not logged out!")
    }
}

fun Route.registerUser(userDataSource: UserDataSource) {
    post("v1/register") {
        val request = call.receive<Map<String, String>>()
        val firstName = request["firstName"]!!
        val lastName = request["lastName"]!!
        val login = request["login"]!!
        val password = request["password"]!!

        val newUser = userDataSource.insertUser(generateId(), firstName, lastName, login, password)
        if (newUser == null) {
            call.respond(HttpStatusCode.BadRequest, "User already created!")
            return@post
        } else {
            call.respond(newUser)
        }
    }

    get("v1/get-token/{deviceId}") {
        val deviceId = call.parameters["deviceId"]
        if (deviceId.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "Path cannot be null!")
            return@get
        } else {
            val tokenResponse = userDataSource.insertToken(deviceId)
            call.respond(tokenResponse)
        }
    }
}