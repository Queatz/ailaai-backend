package com.queatz.api

import com.queatz.MessagePushData
import com.queatz.PushAction
import com.queatz.PushData
import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.push
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
                val me = me

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    db.insert(Message(member.to?.asKey(), member.id, message.text))

                    member.seen = Clock.System.now()
                    db.update(member)

                    val group = db.document(Group::class, member.to!!)!!
                    group.seen = Clock.System.now()
                    db.update(group)

                    val pushData = PushData(PushAction.Message, MessagePushData(
                        Group().apply { id = group.id },
                        Person(name = me.name).apply { id = me.id },
                        Message(text = message.text)
                    ))

                    db.memberDevices(group.id!!).filter {
                        it.member?.id != member.id
                    }.forEach {
                        it.devices?.forEach { device ->
                            push.sendPush(device, pushData)
                        }
                    }

                    HttpStatusCode.OK
                }
            }
        }
    }
}
