package com.queatz.api

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.meRoutes() {
    authenticate {
        get("/me") {

        }

        post("/me") {

        }
    }
}
