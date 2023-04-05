package com.queatz.api

import com.queatz.db.peopleWithName
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.peopleRoutes() {
    authenticate {
        get("/people") {
            respond {
                val search = call.parameters["search"]?.takeIf { it.isNotBlank() }
                db.peopleWithName(
                    search ?: return@respond HttpStatusCode.BadRequest.description("Missing 'search' parameter"),
                    me.geo
                )
            }
        }
    }
}
