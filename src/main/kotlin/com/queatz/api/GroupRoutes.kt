package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun Route.groupRoutes() {
    authenticate {
        get("/groups") {
            respond {
                db.groups(me.id!!)
            }
        }

        get("/groups/{id}") {
            respond {
                db.group(me.id!!, call.parameters["id"]!!) ?: HttpStatusCode.NotFound
            }
        }

        get("/groups/{id}/messages") {
            respond {
                db.group(me.id!!, call.parameters["id"]!!)?.let {
                    db.messages(it.group!!.id!!)
                } ?: HttpStatusCode.NotFound
            }
        }

        post("/groups/{id}/messages") {
            respond {
                val message = call.receive<Message>()
                val member = db.member(me.id!!, call.parameters["id"]!!)

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    db.insert(Message(member.to?.asKey(), member.id, message.text))

                    member.seen = Clock.System.now()
                    db.update(member)

                    val group = db.document(Group::class, member.to!!)!!
                    group.seen = Clock.System.now()
                    db.update(group)

                    // Todo: send push notification or mqtt

                    HttpStatusCode.OK
                }
            }
        }
    }
}
