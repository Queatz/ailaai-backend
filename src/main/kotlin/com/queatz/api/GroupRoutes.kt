package com.queatz.api

import com.queatz.MessagePushData
import com.queatz.PushAction
import com.queatz.PushData
import com.queatz.db.*
import com.queatz.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

data class CreateGroupBody(val people: List<String>)

fun Route.groupRoutes() {
    authenticate {
        get("/groups") {
            respond {
                val me = me
                me.seen = Clock.System.now()
                db.update(me)

                db.groups(me.id!!)
            }
        }

        post("/groups") {
            respond {
                call.receive<CreateGroupBody>().let {
                    val people = it.people + me.id!!

                    if (people.size > 20) {
                        HttpStatusCode.BadRequest.description("Too many people")
                    } else db.group(it.people) ?: app.createGroup(people)
                }
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
                    db.insert(Message(member.to?.asKey(), member.id, message.text, message.attachment))

                    member.seen = Clock.System.now()
                    db.update(member)

                    me.seen = Clock.System.now()
                    db.update(me)

                    val group = db.document(Group::class, member.to!!)!!
                    group.seen = Clock.System.now()
                    db.update(group)

                    val pushData = PushData(
                        PushAction.Message, MessagePushData(
                            Group().apply { id = group.id },
                            Person(name = me.name).apply { id = me.id },
                            Message(text = message.text?.ellipsize())
                        )
                    )

                    db.memberDevices(group.id!!).filter {
                        it.member?.id != member.id
                    }.apply {
                        filter { it.member?.hide == true }.forEach {
                            it.member!!.hide = false
                            db.update(it.member!!)
                        }

                        forEach {
                            it.devices?.forEach { device ->
                                push.sendPush(device, pushData)
                            }
                        }
                    }

                    HttpStatusCode.OK
                }
            }
        }
    }
}

fun String.ellipsize(maxLength: Int = 128) = if (length <= maxLength) this else this.take(maxLength - 1) + "…"
