package com.queatz.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.cardRoutes() {
    get("/cards") {
        val geo = call.parameters["geo"]!!.split(",")
        val search = call.parameters["search"]


    }

    authenticate {
        post("/cards") {

        }
    }
}
