package com.queatz.db

import com.arangodb.entity.CollectionType
import com.arangodb.model.FulltextIndexOptions
import com.arangodb.model.GeoIndexOptions
import com.arangodb.model.PersistentIndexOptions

fun collections() = listOf(
    Person::class.db {
        ensurePersistentIndex(listOf(Person::name.name), PersistentIndexOptions())
    },
    Transfer::class.db {
        ensurePersistentIndex(listOf(Transfer::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Transfer::code.name), PersistentIndexOptions())
    },
    Device::class.db {
        ensurePersistentIndex(listOf(Device::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Device::type.name, Device::token.name), PersistentIndexOptions())
    },
    Settings::class.db {
        ensurePersistentIndex(listOf(Settings::person.name), PersistentIndexOptions())
    },
    Presence::class.db {
        ensurePersistentIndex(listOf(Presence::person.name), PersistentIndexOptions())
    },
    Profile::class.db {
        ensurePersistentIndex(listOf(Profile::person.name), PersistentIndexOptions())
    },
    Invite::class.db {
        ensurePersistentIndex(listOf(Invite::code.name), PersistentIndexOptions())
    },
    Save::class.db {
        ensurePersistentIndex(listOf(Save::person.name), PersistentIndexOptions())
    },
    Search::class.db {
        ensurePersistentIndex(listOf(Search::search.name), PersistentIndexOptions())
    },
    Story::class.db {
        ensurePersistentIndex(listOf(Story::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Story::title.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Story::url.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Story::published.name), PersistentIndexOptions())
    },
    StoryDraft::class.db {
        ensurePersistentIndex(listOf(StoryDraft::story.name), PersistentIndexOptions())

    },
    Card::class.db {
        ensurePersistentIndex(listOf(Card::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::parent.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::active.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::name.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::location.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::collaborators.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::equipped.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::conversation.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::offline.name), PersistentIndexOptions())
        ensureFulltextIndex(listOf(Card::conversation.name), FulltextIndexOptions())
        ensureGeoIndex(listOf(Card::geo.name), GeoIndexOptions())
    },
    Group::class.db {
        ensurePersistentIndex(listOf(Group::seen.name), PersistentIndexOptions())
    },
    Member::class.db(CollectionType.EDGES, listOf(Group::class, Person::class)) {
        ensurePersistentIndex(listOf(Member::gone.name), PersistentIndexOptions())
    },
    Message::class.db {},
    Crash::class.db {},
    Report::class.db {
        ensurePersistentIndex(listOf(Report::type.name), PersistentIndexOptions())
    },
    AppFeedback::class.db {},
    Sticker::class.db {
        ensurePersistentIndex(listOf(Sticker::pack.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Sticker::name.name), PersistentIndexOptions())
    },
    StickerPack::class.db {
        ensurePersistentIndex(listOf(StickerPack::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(StickerPack::active.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(StickerPack::description.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(StickerPack::name.name), PersistentIndexOptions())
    },
    StickerPackSave::class.db(CollectionType.EDGES, listOf(StickerPack::class, Person::class)) {
    }
)
