package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import com.queatz.receiveFile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
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

        get("/me/stories") {
            respond {
                db.storiesOfPerson(me.id!!)
            }
        }

        get("/me/presence") {
            respond {
                val me = me
                db.presenceOfPerson(me.id!!).apply {
                    if (me.geo != null) {
                        unreadStoriesCount = db.countStories(me.geo!!, me.id!!, nearbyMaxDistance = 500_000.0, after = readStoriesUntil)
                    }
                }
            }
        }

        post("/me/presence/read-stories") {
            respond {
                val me = me
                val presence = db.presenceOfPerson(me.id!!).apply {
                    readStoriesUntil = Clock.System.now()
                }
                db.update(presence).apply {
                    if (me.geo != null) {
                        unreadStoriesCount = db.countStories(me.geo!!, me.id!!, nearbyMaxDistance = 500_000.0, after = readStoriesUntil)
                    }
                }
            }
        }

        get("/me/profile") {
            respond {
                db.profile(me.id!!)
            }
        }

        post("/me/profile") {
            respond {
                val update = call.receive<Profile>()
                val profile = db.profile(me.id!!)

                if (update.about != null) {
                    profile.about = update.about?.trim()
                }

                if (update.photo != null) {
                    profile.photo = update.photo
                    profile.video = null
                }

                if (update.video != null) {
                    profile.video = update.video
                    profile.photo = null
                }

                db.update(profile)
            }
        }

        post("/me/profile/photo") {
            respond {
                val person = me

                call.receiveFile("photo", "profile-${person.id!!}") { it, _ ->
                    val profile = db.profile(me.id!!)
                    profile.photo = it
                    profile.video = null
                    db.update(profile)
                }
            }
        }

        post("/me/profile/video") {
            respond {
                val person = me

                call.receiveFile("photo", "profile-${person.id!!}") { it, _ ->
                    val profile = db.profile(me.id!!)
                    profile.video = it
                    profile.photo = null
                    db.update(profile)
                }
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
                call.receiveFile("photo", "person-${person.id}") { it, _ ->
                    person.photo = it
                    db.update(person)
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
