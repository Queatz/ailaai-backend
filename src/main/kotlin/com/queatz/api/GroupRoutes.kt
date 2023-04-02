package com.queatz.api

import com.queatz.MessagePushData
import com.queatz.PushAction
import com.queatz.PushData
import com.queatz.db.*
import com.queatz.plugins.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import kotlin.random.Random

data class CreateGroupBody(val people: List<String>, val reuse: Boolean = false)

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
                } ?: HttpStatusCode.NotFound
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
                        db.update(group)
                    } else {
                        HttpStatusCode.BadRequest
                    }
                }
            }
        }

        get("/groups/{id}/messages") {
            respond {
                db.group(me.id!!, call.parameters["id"]!!)?.let {
                    db.messages(it.group!!.id!!)
                } ?: HttpStatusCode.NotFound
            }
        }

        get("/groups/{id}/members") {
            respond {
                // todo verify I'm in this group before returning members
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
                    val parts = call.receiveMultipart().readAllParts()

                    val photo = parts.find { it.name == "photo" } as? PartData.FileItem

                    if (photo == null) {
                        HttpStatusCode.BadRequest.description("Missing 'photo'")
                    } else {
                        if (!File("./static/photos").isDirectory) {
                            File("./static/photos").mkdirs()
                        }

                        val group = db.document(Group::class, member.to!!)!!

                        val fileName = "group-${group.id}-${Random.nextInt(10000000, 99999999)}-${photo.originalFileName}"
                        val file = File("./static/photos/${fileName}")

                        withContext(Dispatchers.IO) {
                            file.outputStream().write(photo.streamProvider().readBytes())
                        }

                        val photoUrl = "/static/photos/${fileName}"
                        val message = db.insert(Message(member.to?.asKey(), member.id, null, json.toJson(PhotosAttachment(
                            photos = listOf(photoUrl)
                        ))))

                        updateSeen(person, member, group)
                        notifyMessage(me, member, group, message)

                        HttpStatusCode.NoContent
                    }
                }
            }
        }
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
