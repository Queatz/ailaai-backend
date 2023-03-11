package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.app
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
        post("/members") {
            respond {
                val newMember = call.receive<Member>()
                if (newMember.id != null) {
                    HttpStatusCode.BadRequest.description("'id' cannot be specified")
                } else if (newMember.from == null) {
                    HttpStatusCode.BadRequest.description("'from' is missing")
                } else if (newMember.to == null) {
                    HttpStatusCode.BadRequest.description("'to' is missing")
                } else if (db.member(newMember.from!!, newMember.to!!) != null) {
                    HttpStatusCode.Forbidden.description("Member is already in group")
                } else if (db.group(me.id!!, newMember.to!!) == null) {
                    HttpStatusCode.NotFound.description("Group not found")
                } else if (db.document(Person::class, newMember.from!!) == null) {
                    HttpStatusCode.NotFound.description("Person not found")
                } else {
                    app.createMember(newMember.from!!, newMember.to!!)
                }
            }
        }

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

        post("/members/{id}/delete") {
            respond {
                val member = db.document(Member::class, call.parameters["id"]!!)

                if (member == null) {
                    HttpStatusCode.NotFound
                } else if (member.from != me.id!!.asId(Person::class)) {
                    HttpStatusCode.Forbidden
                } else {
                    member.hide = true
                    member.gone = true
                    db.update(member)
                    HttpStatusCode.NoContent
                }
            }
        }
    }
}
