package com.queatz

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.queatz.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureHTTP()
        configureSerialization()
        configureSecurity()
    }.start(wait = true)
}
