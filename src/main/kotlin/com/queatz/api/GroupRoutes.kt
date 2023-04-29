package com.queatz.api

import com.queatz.MessagePushData
import com.queatz.PushAction
import com.queatz.PushData
import com.queatz.db.*
import com.queatz.plugins.*
import com.queatz.receivePhotos
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toInstant

data class CreateGroupBody(val people: List<String>, val reuse: Boolean = false)

fun Route.groupRoutes() {
    authenticate {
        get("/groups") {
            respond {
                val me = me
                me.seen = Clock.System.now()
                db.update(me)

                db.groups(me.id!!).forApi()
            }
        }

        post("/groups") {
            respond {
                call.receive<CreateGroupBody>().let {
                    val people = (it.people + me.id!!).toSet().toList()

                    if (people.size > 50) {
                        HttpStatusCode.BadRequest.description("Too many people")
                    } else if (it.reuse) {
                        db.group(it.people) ?: app.createGroup(people, hosts = me.id!!.let(::listOf))
                    } else {
                        app.createGroup(people, hosts = me.id!!.let(::listOf))
                    }
                }
            }
        }

        get("/groups/{id}") {
            respond {
                val person = me
                db.group(me.id!!, call.parameters["id"]!!)?.also { group ->
                    group.members?.find { it.person?.id == person.id }?.member?.let { member ->
                        member.seen = Clock.System.now()
                        db.update(member)
                    }
                }?.forApi() ?: HttpStatusCode.NotFound
            }
        }


        post("/groups/{id}") {
            respond {
                val groupUpdated = call.receive<Group>()
                val member = db.member(me.id!!, call.parameters["id"]!!)

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    val group = db.document(Group::class, member.to!!) ?: return@respond HttpStatusCode.NotFound

                    if (groupUpdated.name != null) {
                        group.name = groupUpdated.name
                    }

                    if (groupUpdated.description != null) {
                        group.description = groupUpdated.description
                    }

                    db.update(group)
                }
            }
        }

        get("/groups/{id}/messages") {
            respond {
                db.group(me.id!!, call.parameters["id"]!!)?.let {
                    db.messages(
                        it.group!!.id!!,
                        call.parameters["before"]?.toInstant(),
                        call.parameters["limit"]?.toInt() ?: 20
                    )
                } ?: HttpStatusCode.NotFound
            }
        }

//        get("/groups/{id}/members") {
//            respond {
//                // todo verify I'm in this group before returning members
//                db.group(me.id!!, call.parameters["id"]!!)?.let {
//                    db.members(it.group!!.id!!)
//                } ?: HttpStatusCode.NotFound
//            }
//        }

        post("/groups/{id}/messages") {
            respond {
                val message = call.receive<Message>()
                val member = db.member(me.id!!, call.parameters["id"]!!)
                val me = me

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    db.insert(Message(member.to?.asKey(), member.id, message.text, message.attachment))
                    val group = db.document(Group::class, member.to!!)!!

                    updateSeen(me, member, group)
                    notifyMessage(me, member, group, message)

                    HttpStatusCode.OK
                }
            }
        }

        post("/groups/{id}/photos") {
            respond {
                val person = me
                val member = db.member(person.id!!, call.parameters["id"]!!)

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    val group = db.document(Group::class, member.to!!)!!
                    call.receivePhotos("group-${group.id}") { photosUrls ->
                        val message = db.insert(
                            Message(
                                member.to?.asKey(), member.id, null, json.toJson(
                                    PhotosAttachment(
                                        photos = photosUrls
                                    )
                                )
                            )
                        )

                        updateSeen(person, member, group)
                        notifyMessage(me, member, group, message)
                    }
                }
            }
        }
    }
}


private fun List<GroupExtended>.forApi() = onEach {
    it.forApi()
}

private fun GroupExtended.forApi() = also {
    it.members?.onEach {
        it.person?.geo = null
    }
}

private fun notifyMessage(me: Person, member: Member, group: Group, message: Message) {
    val pushData = PushData(
        PushAction.Message, MessagePushData(
            Group().apply { id = group.id },
            Person(name = me.name).apply { id = me.id },
            Message(text = message.text?.ellipsize(), attachment = message.attachment)
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
}

private fun updateSeen(me: Person, member: Member, group: Group) {
    member.seen = Clock.System.now()
    db.update(member)

    me.seen = Clock.System.now()
    db.update(me)

    group.seen = Clock.System.now()
    db.update(group)
}

fun String.ellipsize(maxLength: Int = 128) = if (length <= maxLength) this else this.take(maxLength - 1) + "…"
