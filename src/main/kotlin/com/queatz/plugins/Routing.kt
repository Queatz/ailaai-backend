package com.queatz.plugins

import com.queatz.api.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.launch

fun Application.configureRouting() {
    launch {
        // Delete all expired invite codes every hour
    }

    routing {
        get("/hi") { call.respondText("{\"hi\": true}") }
        signRoutes()
        groupRoutes()
        meRoutes()
        messageRoutes()
        cardRoutes()
    }
}

suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.respond(block: () -> T) = call.respond(block())
