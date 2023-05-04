package com.queatz.api

import com.queatz.db.Crash
import com.queatz.db.recentCrashes
import com.queatz.plugins.db
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.crashRoutes() {
    post("/crash") {
        respond {
            db.insert(
                Crash(
                    details = call.receive<Crash>().details
                )
            )
            HttpStatusCode.NoContent
        }
    }
    get("/crash") {
        respond {
            db.recentCrashes()
        }
    }
}
