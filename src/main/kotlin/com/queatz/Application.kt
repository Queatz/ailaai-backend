package com.queatz

import com.queatz.plugins.configureHTTP
import com.queatz.plugins.configureRouting
import com.queatz.plugins.configureSecurity
import com.queatz.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}


fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureSecurity()
    configureRouting()
}

