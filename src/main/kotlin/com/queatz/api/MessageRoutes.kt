package com.queatz.api

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.messageRoutes() {
    authenticate {
        get("/messages/{id}/delete") {

        }

        get("/messages/{id}/report") {

        }
    }
}
