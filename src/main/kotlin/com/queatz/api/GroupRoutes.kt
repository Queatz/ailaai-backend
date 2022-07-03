package com.queatz.api

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.groupRoutes() {
    authenticate {
        get("/groups") {

        }

        get("/group/{id}") {

        }
    }
}
