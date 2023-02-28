package org.tamtam.db

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.tamtam.cache.InMemoryCache
import org.tamtam.model.exception.ExceptionResponse
import org.tamtam.model.login.TokenRemoteModel
import org.tamtam.model.story.StoriesModel

class MongoStoriesDataSource(db: CoroutineDatabase) : StoriesDataSource {

    private val tokens = db.getCollection<TokenRemoteModel>("tokens_collection")

    override suspend fun getStories(): List<StoriesModel> {
        return InMemoryCache.getStories()
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