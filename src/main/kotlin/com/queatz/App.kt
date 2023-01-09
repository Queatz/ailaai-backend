package com.queatz

import com.queatz.db.Group
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.plugins.db

class App {
    fun createGroup(people: List<String>): Group = createGroup(*people.toTypedArray())

    fun createGroup(vararg person: String): Group =
        db.insert(Group())
            .also {
                val group = it.id!!.asId(Group::class)

                person.distinct().forEach {
                    db.insert(
                        Member(
                            from = it.asId(Person::class),
                            to = group
                        )
                    )
                }
            }
}
