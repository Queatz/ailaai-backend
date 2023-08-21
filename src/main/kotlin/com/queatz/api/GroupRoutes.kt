package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.*
import com.queatz.receiveFile
import com.queatz.receiveFiles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
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
                db.group(me.id!!, parameter("id"))?.also { group ->
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
                val member = db.member(me.id!!, parameter("id"))

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
                db.group(me.id!!, parameter("id")).let {
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
//                db.group(me.id!!, parameter("id"))?.let {
//                    db.members(it.group!!.id!!)
//                } ?: HttpStatusCode.NotFound
//            }
//        }

        post("/groups/{id}/messages") {
            respond {
                val message = call.receive<Message>()
                val member = db.member(me.id!!, parameter("id"))
                val me = me

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    db.insert(Message(member.to?.asKey(), member.id, message.text, message.attachment, message.attachments))
                    val group = db.document(Group::class, member.to!!)!!

                    updateSeen(me, member, group)
                    notifyMessage(me, group, message)

                    HttpStatusCode.OK
                }
            }
        }

        post("/groups/{id}/photos") {
            respond {
                val person = me
                val member = db.member(person.id!!, parameter("id"))

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    val group = db.document(Group::class, member.to!!)!!
                    call.receiveFiles("photo", "group-${group.id}") { photosUrls, params ->
                        val message = db.insert(
                            Message(
                                member.to?.asKey(), member.id, null, json.encodeToString(
                                    PhotosAttachment(
                                        photos = photosUrls
                                    )
                                ),
                                attachments = params["message"]?.let { json.decodeFromString<Message>(it,) }?.attachments
                            )
                        )

                        updateSeen(person, member, group)
                        notifyMessage(me, group, message)
                    }
                }
            }
        }

        post("/groups/{id}/audio") {
            respond {
                val person = me
                val member = db.member(person.id!!, parameter("id"))

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    val group = db.document(Group::class, member.to!!)!!
                    call.receiveFile("audio", "group-${group.id}") { url, params ->
                        val message = db.insert(
                            Message(
                                member.to?.asKey(), member.id, null, json.encodeToString(
                                    AudioAttachment(
                                        audio = url
                                    )
                                ),
                                attachments = params.get("message")?.let { json.decodeFromString<Message>(it) }?.attachments
                            )
                        )

                        updateSeen(person, member, group)
                        notifyMessage(me, group, message)
                    }
                }
            }
        }
    }
}


fun List<GroupExtended>.forApi() = onEach {
    it.forApi()
}

fun GroupExtended.forApi() = also {
    it.members?.onEach {
        it.person?.geo = null
    }
}

private fun notifyMessage(me: Person, group: Group, message: Message) {
    notify.message(group, me, Message(text = message.text?.ellipsize(), attachment = message.attachment))
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
