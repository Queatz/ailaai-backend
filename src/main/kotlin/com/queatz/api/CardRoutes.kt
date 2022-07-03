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
                db.insert(Card(person.id!!.asId(Person::class), person.name, active = false))
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

        post("/cards/{id}") {
            respond {
                val card = db.document(Card::class, call.parameters["id"]!!)
                val person = me

                if (card == null) {
                    HttpStatusCode.NotFound
                } else if (card.person!!.asKey() != person.id) {
                    HttpStatusCode.Forbidden
                } else {
                    val update = call.receive<Card>()

                    fun <T> check(prop: KMutableProperty1<Card, T>) { if (prop.get(update) != null) prop.set(card, prop.get(update)) }

                    check(Card::active)
                    check(Card::geo)
                    check(Card::location)
                    check(Card::name)
                    check(Card::conversation)
                    check(Card::photo)

                    db.update(card)
                }
            }
        }
    }
}
