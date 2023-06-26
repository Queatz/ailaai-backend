package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.respond
import io.ktor.server.application.*
import io.ktor.server.routing.*

data class AppStats(
    val activePeople30Days: Int,
    val activePeople7Days: Int,
    val activePeople24Hours: Int,
    val newPeople30Days: Int,
    val newPeople7Days: Int,
    val newPeople24Hours: Int,
    val totalPeople: Int,
    val totalDraftCards: Int,
    val totalPublishedCards: Int
)

fun Route.statsRoutes() {
    get("/stats") {
        respond {
            AppStats(
                activePeople30Days = db.activePeople(days = 30),
                activePeople7Days = db.activePeople(days = 7),
                activePeople24Hours = db.activePeople(days = 1),
                newPeople30Days = db.newPeople(days = 30),
                newPeople7Days = db.newPeople(days = 7),
                newPeople24Hours = db.newPeople(days = 1),
                totalPeople = db.totalPeople,
                totalDraftCards = db.totalDraftCards,
                totalPublishedCards = db.totalPublishedCards,
            )
        }
    }
    get("/stats/searches") {
        respond {
            db.recentSearches(call.parameters["limit"]?.toInt() ?: 20)
        }
    }
    get("/stats/feedback") {
        respond {
            db.recentFeedback(call.parameters["limit"]?.toInt() ?: 20)
        }
    }
}
