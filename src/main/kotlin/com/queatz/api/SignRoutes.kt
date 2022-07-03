package com.queatz.api

import com.queatz.db.Person
import com.queatz.db.invite
import com.queatz.db.totalPeople
import com.queatz.plugins.db
import com.queatz.plugins.jwt
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.signRoutes() {
    post("/sign/up") {
        respond {
            val code = call.receive<SignUpRequest>().code

            if (code == "000000" && db.totalPeople == 0) {
                val person = db.insert(Person())
                TokenResponse(jwt(person.id!!))
            } else {
                val invite = db.invite(code)

                if (invite == null) {
                    HttpStatusCode.Unauthorized
                } else {
                    val person = db.insert(Person().apply {
                        inviter = invite.person
                    })

                    db.delete(invite)

                    TokenResponse(jwt(person.id!!))
                }
            }
        }
    }

    post("/sign/on") {
        respond {
            val credentials = call.receive<SignOnRequest>()

            if (credentials.code != null) {
                // Check code
            } else {
                // Send email
            }
        }
    }
}
