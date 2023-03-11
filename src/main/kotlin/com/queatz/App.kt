package com.queatz

import com.queatz.db.Group
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.plugins.db
import kotlinx.datetime.Clock

class App {
    fun createGroup(people: List<String>): Group = createGroup(*people.toTypedArray())

    fun createGroup(vararg person: String): Group =
        db.insert(Group(seen = Clock.System.now()))
            .also {
                val group = it.id!!.asId(Group::class)

                person.distinct().forEach {
                    createMember(it, group)
                }
            }

    fun createMember(person: String, group: String) = db.insert(
        Member(
            from = person.asId(Person::class),
            to = group.asId(Group::class)
        )
    )
}
