package com.queatz.api

import com.queatz.db.Card
import com.queatz.db.cards
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.cardRoutes() {
    authenticate {
        get("/cards") {
            respond {
                db.cards(
                    call.parameters["geo"]!!.split(",").map { it.toDouble() },
                    call.parameters["search"],
                    call.parameters["offset"]?.toInt() ?: 0,
                    call.parameters["limit"]?.toInt() ?: 20
                )
            }
        }

        post("/cards") {
            respond {
                val person = me
                db.insert(Card(person.id, person.name, active = false))
            }
        }
    }
}
