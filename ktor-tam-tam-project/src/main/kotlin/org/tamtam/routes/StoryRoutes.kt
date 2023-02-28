package org.tamtam.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.tamtam.cache.InMemoryCache
import org.tamtam.db.StoriesDataSource
import org.tamtam.db.UserDataSource

fun Route.getStories(dataSource: StoriesDataSource) {
    get("v1/stories") {
        if (!dataSource.checkToken(call)) return@get

        val stories = dataSource.getStories()
        call.respond(stories)
    }
}