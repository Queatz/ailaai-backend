package com.queatz.api

import com.queatz.db.Invite
import com.queatz.db.Person
import com.queatz.db.cardsOfPerson
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlin.random.Random

fun Route.meRoutes() {
    authenticate {
        get("/me") {
            respond { me }
        }

        get("/me/cards") {
            respond {
                db.cardsOfPerson(me.id!!)
            }
        }

        post("/me") {
            respond {
                val update = call.receive<Person>()
                val person = me

                if (!update.name.isNullOrBlank()) {
                    person.name = update.name
                }

                if (!update.photo.isNullOrBlank()) {
                    person.photo = update.photo
                }

                db.update(person)
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

private fun Random.Default.code() = (1..6).joinToString("") { "${nextInt(9)}" }
