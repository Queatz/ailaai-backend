package com.queatz.api

import com.queatz.db.Person
import com.queatz.db.invite
import com.queatz.db.totalPeople
import com.queatz.db.transferWithCode
import com.queatz.plugins.app
import com.queatz.plugins.db
import com.queatz.plugins.jwt
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

fun Route.signRoutes() {
    post("/sign/up") {
        respond {
            val code = call.receive<SignUpRequest>().code

            if (code == "000000" && db.totalPeople == 0) {
                val person = db.insert(Person(seen = Clock.System.now()))
                TokenResponse(jwt(person.id!!))
            } else {
                val invite = db.invite(code)

                if (invite == null) {
                    HttpStatusCode.NotFound.description("Invite not found")
                } else if (invite.createdAt!! < Clock.System.now().minus(2.days)) {
                    db.delete(invite)
                    HttpStatusCode.NotFound.description("Invite expired")
                } else {
                    val person = db.insert(Person(seen = Clock.System.now(), inviter = invite.person))

                    db.delete(invite)

                    app.createGroup(person.id!!, invite.person!!)

                    TokenResponse(jwt(person.id!!))
                }
            }
        }
    }

    post("/sign/in") {
        respond {
            val code = call.receive<SignInRequest>().code

            val transfer = db.transferWithCode(code)

            if (transfer == null) {
                HttpStatusCode.NotFound.description("Transfer code '$code' not found")
            } else {
                val person = db.document(Person::class, transfer.person!!)

                if (person == null) {
                    HttpStatusCode.NotFound.description("Transfer code is orphaned")
                } else {
                    db.delete(transfer)

                    TokenResponse(jwt(person.id!!))
                }
            }
        }
    }
}
