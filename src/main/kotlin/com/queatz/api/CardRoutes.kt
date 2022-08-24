package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.app
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
import kotlin.reflect.KMutableProperty1

fun Route.cardRoutes() {
    authenticate {
        get("/cards") {
            respond {
                db.cards(
                    call.parameters["geo"]!!.split(",").map { it.toDouble() },
                    call.parameters["search"],
                    call.parameters["offset"]?.toInt() ?: 0,
                    call.parameters["limit"]?.toInt() ?: 20
                )
            }
        }

        post("/cards") {
            respond {
                val person = me
                val card = call.receiveOrNull<Card>()
                db.insert(Card(person.id!!, name = person.name, parent = card?.parent, active = false))
            }
        }

        get("/cards/{id}") {
            respond {
                val card = db.document(Card::class, call.parameters["id"]!!)

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() == me.id || card.active == true) {
                    card
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }

        get("/cards/{id}/cards") {
            respond {
                val card = db.document(Card::class, call.parameters["id"]!!)

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() == me.id || card.active == true) {
                    db.cardsOfCard(card.id!!)
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }

        get("/cards/{id}/group") {
            respond {
                val card = db.document(Card::class, call.parameters["id"]!!)
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else {
                    val group = db.group(listOf(me.id!!, card.person!!)) ?: db.insert(Group())
                        .also {
                            app.createGroup(person.id!!, card.person!!)
                        }

                    // Todo: send message referencing card

                    group
                }
            }
        }

        post("/cards/{id}") {
            respond {
                val card = db.document(Card::class, call.parameters["id"]!!)
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val update = call.receive<Card>()

                    fun <T> check(prop: KMutableProperty1<Card, T>, doOnSet: (() -> Unit)? = null) {
                        if (prop.get(update) != null) {
                            prop.set(card, prop.get(update))
                            doOnSet?.invoke()
                        }
                    }

                    if (update.parent != null) {
                        val parent = db.document(Card::class, call.parameters["id"]!!)

                        if (parent!!.person != card.person) {
                            return@respond HttpStatusCode.Forbidden
                        }
                    }

                    check(Card::active)
                    check(Card::geo) { card.parent = update.parent }
                    check(Card::location)
                    check(Card::name)
                    check(Card::conversation)
                    check(Card::photo)
                    check(Card::parent)

                    db.update(card)
                }
            }
        }

        post("/cards/{id}/photo") {
            respond {
                val card = db.document(Card::class, call.parameters["id"]!!)
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val parts = call.receiveMultipart().readAllParts()

                    val photo = parts.find { it.name == "photo" } as? PartData.FileItem

                    if (photo == null) {
                        HttpStatusCode.BadRequest.description("Missing 'photo'")
                    } else {
                        if (!File("./static/photos").isDirectory) {
                            File("./static/photos").mkdirs()
                        }

                        val fileName = "card-${card.id}-${Random.nextInt(10000000, 99999999)}-${photo.originalFileName}"
                        val file = File("./static/photos/${fileName}")

                        withContext(Dispatchers.IO) {
                            file.outputStream().write(photo.streamProvider().readBytes())
                        }

                        val photoUrl = "/static/photos/${fileName}"
                        card.photo = photoUrl

                        db.update(card)

                        HttpStatusCode.NoContent
                    }
                }
            }
        }

        post("/cards/{id}/delete") {
            respond {
                val card = db.document(Card::class, call.parameters["id"]!!)
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    db.delete(card)
                    HttpStatusCode.NoContent
                }
            }
        }
    }
}
