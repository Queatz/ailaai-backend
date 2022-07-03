package com.queatz.plugins

import com.queatz.api.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureRouting() {
    routing {
        get("/hi") { call.respondText("{\"hi\": true}") }
        signRoutes()
        groupRoutes()
        meRoutes()
        messageRoutes()
        cardRoutes()
    }
}
