package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

data class ProfileStats(
    val friendsCount: Int,
    val cardCount: Int
)

data class PersonProfile(
    val person: Person,
    val profile: Profile,
    val stats: ProfileStats
)

fun Route.peopleRoutes() {
    authenticate {
        get("/people") {
            respond {
                val search = call.parameters["search"]?.takeIf { it.isNotBlank() }
                db.peopleWithName(
                    me.id!!,
                    search ?: return@respond HttpStatusCode.BadRequest.description("Missing 'search' parameter"),
                    me.geo
                ).forApi()
            }
        }

        get("/people/{id}/profile") {
            respond {
                val person = db.document(Person::class, call.parameters["id"]!!)
                    ?: return@respond HttpStatusCode.NotFound

                PersonProfile(
                    person,
                    db.profile(person.id!!),
                    ProfileStats(
                        friendsCount = db.friendsCount(person.id!!),
                        cardCount = db.cardsCount(person.id!!),
                    )
                )
            }
        }

        get("/people/{id}/profile/cards") {
            respond {
                val person = db.document(Person::class, call.parameters["id"]!!)
                    ?: return@respond HttpStatusCode.NotFound

                db.equippedCardsOfPerson(person.id!!, me.id!!)
            }
        }
    }
}

private fun List<Person>.forApi() = onEach {
    it.forApi()
}

private fun Person.forApi() {
    geo = null
}
