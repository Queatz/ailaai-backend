package com.queatz.db

import com.arangodb.entity.CollectionType
import com.arangodb.model.FulltextIndexOptions
import com.arangodb.model.GeoIndexOptions
import com.arangodb.model.PersistentIndexOptions

fun collections() = listOf(
    Person::class.db {},
    Transfer::class.db {
        ensurePersistentIndex(listOf(Transfer::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Transfer::code.name), PersistentIndexOptions())
    },
    Device::class.db {
        ensurePersistentIndex(listOf(Device::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Device::type.name, Device::token.name), PersistentIndexOptions())
    },
    Settings::class.db {},
    Invite::class.db {
        ensurePersistentIndex(listOf(Invite::code.name), PersistentIndexOptions())
    },
    Save::class.db {
        ensurePersistentIndex(listOf(Save::person.name), PersistentIndexOptions())
    },
    Card::class.db {
        ensurePersistentIndex(listOf(Card::person.name), PersistentIndexOptions())
        ensureFulltextIndex(listOf(Card::conversation.name), FulltextIndexOptions())
        ensureGeoIndex(listOf(Card::geo.name), GeoIndexOptions())
    },
    Group::class.db {
        ensurePersistentIndex(listOf(Group::seen.name), PersistentIndexOptions())
    },
    Member::class.db(CollectionType.EDGES, listOf(Group::class, Person::class)) {},
    Message::class.db {}
)
