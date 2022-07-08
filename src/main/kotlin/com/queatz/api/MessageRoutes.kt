package com.queatz.api

import com.queatz.db.Member
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.messageRoutes() {
    authenticate {
        post("/messages/{id}/delete") {
            respond {
                val message = db.document(Message::class, call.parameters["id"]!!)

                if (message == null) {
                    HttpStatusCode.NotFound
                } else {
                    val member = db.document(Member::class, message.member!!)

                    if (member?.from != me.id!!.asId(Person::class)) {
                        HttpStatusCode.Forbidden
                    } else {
                        db.delete(message)
                        HttpStatusCode.NoContent
                    }
                }
            }
        }
    }
}
