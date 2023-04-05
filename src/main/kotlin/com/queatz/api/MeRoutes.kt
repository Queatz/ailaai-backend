package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

private data class LeaveCollaborationBody(val card: String)

fun Route.meRoutes() {
    authenticate {
        get("/me") {
            respond { me }
        }

        post("/me/device") {
            respond {
                val device = call.receive<Device>()
                db.updateDevice(me.id!!, device.type!!, device.token!!)
                HttpStatusCode.NoContent
            }
        }

        get("/me/transfer") {
            respond {
                db.transferOfPerson(me.id!!) ?: db.insert(
                    Transfer(
                        person = me.id!!,
                        code = (1..16).token()
                    )
                )
            }
        }

        get("/me/cards") {
            respond {
                db.cardsOfPerson(me.id!!)
            }
        }

        get("/me/collaborations") {
            respond {
                db.collaborationsOfPerson(me.id!!)
            }
        }

        post("/me/collaborations/leave") {
            respond {
                val card = call.receive<LeaveCollaborationBody>().card.let {
                    db.document(Card::class, it)
                } ?: return@respond HttpStatusCode.NotFound.description("Card not found")


                val person = me

                if (card.collaborators?.contains(person.id!!) == true) {
                    card.collaborators = card.collaborators!! - person.id!!
                    db.update(card)
                    notifyCollaboratorRemoved(me, card.people(), card, person.id!!)

                    val childCards = db.allCardsOfCard(card.id!!)
                    childCards.forEach { childCard ->
                        if (childCard.person == person.id) {
                            childCard.parent = null
                            childCard.offline = true
                            db.update(childCard)
                        }
                    }

                    HttpStatusCode.NoContent
                } else {
                    HttpStatusCode.NotFound.description("Collaborator not found")
                }
            }
        }

        get("/me/saved") {
            respond {
                db.savesOfPerson(
                    me.id!!,
                    call.parameters["search"],
                )
            }
        }

        post("/me") {
            respond {
                val update = call.receive<Person>()
                val person = me

                if (!update.name.isNullOrBlank()) {
                    person.name = update.name?.trim()
                }

                if (!update.language.isNullOrBlank()) {
                    person.language = update.language?.trim()
                }

                db.update(person)
            }
        }

        post("/me/photo") {
            respond {
                val person = me

                val parts = call.receiveMultipart().readAllParts()

                val photo = parts.find { it.name == "photo" } as? PartData.FileItem

                if (photo == null) {
                    HttpStatusCode.BadRequest.description("Missing 'photo'")
                } else {
                    if (!File("./static/photos").isDirectory) {
                        File("./static/photos").mkdirs()
                    }

                    val fileName = "person-${person.id}-${Random.nextInt(10000000, 99999999)}-${photo.originalFileName}"
                    val file = File("./static/photos/${fileName}")

                    withContext(Dispatchers.IO) {
                        file.outputStream().write(photo.streamProvider().readBytes())
                    }

                    val photoUrl = "/static/photos/${fileName}"
                    person.photo = photoUrl

                    db.update(person)

                    HttpStatusCode.NoContent
                }
            }
        }

        get("/invite") {
            respond {
                db.insert(
                    Invite(
                        person = me.id!!,
                        code = Random.code()
                    )
                )
            }
        }
    }
}

fun IntRange.token() =
    joinToString("") { Random.nextInt(35).toString(36).let { if (Random.nextBoolean()) it.uppercase() else it } }

private fun Random.Default.code() = (1..6).joinToString("") { "${nextInt(9)}" }
