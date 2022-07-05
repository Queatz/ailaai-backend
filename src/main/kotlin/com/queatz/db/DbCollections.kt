package com.queatz.db

import com.arangodb.entity.CollectionType
import com.arangodb.model.GeoIndexOptions
import com.arangodb.model.PersistentIndexOptions

fun collections() = listOf(
    Person::class.db {},
    Settings::class.db {},
    Invite::class.db {
        ensurePersistentIndex(listOf(Invite::code.name), PersistentIndexOptions())
    },
    Card::class.db {
        ensureGeoIndex(listOf(Card::geo.name), GeoIndexOptions())
    },
    Group::class.db {},
    Member::class.db(CollectionType.EDGES, listOf(Group::class, Person::class)) {},
    Message::class.db {}
)
