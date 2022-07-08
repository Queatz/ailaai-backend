package com.queatz.plugins

import com.queatz.api.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.launch

fun Application.configureRouting() {
    launch {
        // Delete all expired invite codes every hour
    }

    launch {
        push.start()
    }

    routing {
        get("/hi") { call.respondText("{\"hi\": true}") }
        signRoutes()
        meRoutes()
        cardRoutes()
        groupRoutes()
        memberRoutes()
        messageRoutes()

        static("/static") {
            files("static")
        }
    }
}

suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.respond(block: () -> T) {
    call.respond(block())
}
