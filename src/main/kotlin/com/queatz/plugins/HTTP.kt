package com.queatz.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureHTTP() {
    install(CORS) {
        allowNonSimpleContentTypes = true
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // TODO restrict to Android app
    }
    install(Compression)
    install(DefaultHeaders)
}
