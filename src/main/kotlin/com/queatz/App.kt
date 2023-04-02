package com.queatz

import com.queatz.db.Group
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.plugins.db
import kotlinx.datetime.Clock

class App {
    fun createGroup(people: List<String>, hosts: List<String> = emptyList()): Group =
        db.insert(Group(seen = Clock.System.now()))
            .also {
                val group = it.id!!.asId(Group::class)

                people.distinct().forEach {
                    createMember(it, group, host = hosts.contains(it).takeIf { it })
                }
            }

    fun createMember(person: String, group: String, host: Boolean? = null) = db.insert(
        Member(
            from = person.asId(Person::class),
            to = group.asId(Group::class),
            host = host
        )
    )
}
