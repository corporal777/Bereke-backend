package org.tamtam.db

import io.ktor.server.application.*
import org.tamtam.model.story.StoriesModel

interface StoriesDataSource {
    suspend fun getStories(): List<StoriesModel>
    suspend fun checkToken(call: ApplicationCall): Boolean
}