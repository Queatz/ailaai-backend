package com.queatz.api

import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.memberRoutes() {
    authenticate {
        post("/members/{id}") {
            respond {
                val member = db.document(Member::class, call.parameters["id"]!!)

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    if (member.from != me.id!!.asId(Person::class)) {
                        HttpStatusCode.Forbidden
                    } else {
                        val update = call.receive<Member>()

                        if (update.hide != null) {
                            member.hide = update.hide
                            db.update(member)
                        }

                        HttpStatusCode.NoContent
                    }
                }
            }
        }
    }
}
