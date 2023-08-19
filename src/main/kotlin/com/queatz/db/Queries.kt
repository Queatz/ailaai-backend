package com.queatz.db

import kotlinx.datetime.Instant

fun Db.recentCrashes(limit: Int = 50) = list(
    Crash::class,
    """
        for x in @@collection
            sort x.${f(Card::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.recentReports(limit: Int = 50) = list(
    Report::class,
    """
        for x in @@collection
            sort x.${f(Card::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.recentSearches(limit: Int = 50) = list(
    Search::class,
    """
        for x in @@collection
            sort x.${f(Search::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.recentFeedback(limit: Int = 50) = list(
    AppFeedback::class,
    """
        for x in @@collection
            sort x.${f(AppFeedback::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "limit" to limit
    )
)

fun Db.activePeople(days: Int) = query(
        Int::class,
        """
            return count(
                for x in `${Person::class.collection()}`
                    filter x.${f(Person::seen)} != null
                        and x.${f(Person::seen)} >= date_subtract(DATE_NOW(), @days, 'day')
                    return true
            )
        """,
        mapOf(
            "days" to days
        )
    ).first()!!

fun Db.newPeople(days: Int) = query(
        Int::class,
        """
            return count(
                for x in `${Person::class.collection()}`
                    filter x.${f(Person::createdAt)} != null
                        and x.${f(Person::createdAt)} >= date_subtract(DATE_NOW(), @days, 'day')
                    return true
            )
        """,
    mapOf(
        "days" to days
    )
).first()!!

val Db.totalPeople
    get() = query(
        Int::class,
        """
            return count(`${Person::class.collection()}`)
        """
    ).first()!!

val Db.totalDraftCards
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Card::class.collection()}`
                    filter x.${f(Card::active)} != true
                    return true
            )
        """
    ).first()!!

val Db.totalPublishedCards
    get() = query(
        Int::class,
        """
            return count(
                for x in `${Card::class.collection()}`
                    filter x.${f(Card::active)} == true
                    return true
            )
        """
    ).first()!!

/**
 * @code The code of the invite to fetch
 */
fun Db.invite(code: String) = one(
    Invite::class,
    """
        for x in @@collection
            filter x.${f(Invite::code)} == @code
            return x
    """.trimIndent(),
    mapOf(
        "code" to code
    )
)

/**
 * @token The token
 */
fun Db.linkDeviceToken(token: String) = one(
    LinkDeviceToken::class,
    """
        for x in @@collection
            filter x.${f(LinkDeviceToken::token)} == @token
            return x
    """.trimIndent(),
    mapOf(
        "token" to token
    )
)

fun Db.friendsCount(person: String) = query(
    Int::class,
    """
            return count(
                for group in outbound @person graph `${Member::class.graph()}`
                    for friend, member in inbound group graph `${Member::class.graph()}`
                        filter friend.${f(Person::source)} != ${v(PersonSource.Web)}
                            and member.${f(Member::gone)} != true
                            and member._from != @person
                        return distinct friend
            )
        """,
    mapOf(
        "person" to person.asId(Person::class)
    )
).first()!!

fun Db.cardsCount(person: String) = query(
    Int::class,
    """
            return count(
                for card in `${Card::class.collection()}`
                    filter card.${f(Card::person)} == @person
                        and card.${f(Card::active)} == true
                    return distinct card
            )
        """,
    mapOf(
        "person" to person
    )
).first()!!

/**
 * Find people matching @name that are not connected with @person sorted by distance from @geo.
 */
fun Db.peopleWithName(person: String, name: String, geo: List<Double>? = null, limit: Int = 20) = list(
    Person::class,
    """
        for x in @@collection
            filter lower(x.${f(Person::name)}) == @name
                and first(
                    for group, edge in outbound @person graph `${Member::class.graph()}`
                        filter edge.${f(Member::hide)} != true 
                            and edge.${f(Member::gone)} != true
                        for otherPerson, otherEdge in inbound group graph `${Member::class.graph()}`
                            filter otherEdge._to == group._id
                                and otherPerson._id == x._id
                                and otherEdge.${f(Member::gone)} != true
                            limit 1
                            return true
                ) != true
            let d = x.${f(Person::geo)} == null || @geo == null ? null : distance(x.${f(Person::geo)}[0], x.${f(Person::geo)}[1], @geo[0], @geo[1])
            sort d
            sort d == null
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "name" to name.lowercase(),
        "geo" to geo,
        "limit" to limit
    )
)

/**
 * @person The current user
 */
fun Db.cardsOfPerson(person: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
            sort x.${f(Card::createdAt)} desc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.withAuthors(storyVal: String) = """
    merge(
        $storyVal,
        {
            ${f(Story::authors)}: (
                for author in ${Person::class.collection()}
                    filter author._key == $storyVal.${f(Story::person)}
                    return author
                )
        }
    )
""".trimIndent()

/**
 * @story The story id
 */
fun Db.story(story: String) = one(
    Story::class,
    """
        for x in @@collection
            filter x._key == @story
            limit 1
            return ${withAuthors("x")}
    """.trimIndent(),
    mapOf(
        "story" to story
    )
)

/**
 * @story The story id
 */
fun Db.storyByUrl(url: String) = one(
    Story::class,
    """
        for x in @@collection
            filter x._key == @url
                or x.${f(Story::url)} == @url
            limit 1
            return ${withAuthors("x")}
    """.trimIndent(),
    mapOf(
        "url" to url
    )
)

/**
 * @person The current user
 * @story The story id
 */
fun Db.storyDraft(person: String, story: String) = one(
    StoryDraft::class,
    """
        for x in @@collection
            filter x.${f(StoryDraft::story)} == @story
                and first(
                    for story in ${Story::class.collection()}
                        filter story._key == x.${f(StoryDraft::story)}
                        return story
                    ).${f(Story::person)} == @person
            return x
    """.trimIndent(),
    mapOf(
        "person" to person,
        "story" to story
    )
)

/**
 * @person The current user
 */
fun Db.storiesOfPerson(person: String) = list(
    Story::class,
    """
        for x in @@collection
            filter x.${f(Story::person)} == @person
            sort x.${f(Story::createdAt)} desc
            return ${withAuthors("x")}
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

/**
 * @geo The geolocation bias
 * @person The current user
 */
fun Db.stories(geo: List<Double>, person: String, nearbyMaxDistance: Double, offset: Int, limit: Int) = list(
    Story::class,
    """
        for x in @@collection
            filter x.${f(Story::published)} == true
            let d = x.${f(Story::geo)} == null ? null : distance(x.${f(Story::geo)}[0], x.${f(Story::geo)}[1], @geo[0], @geo[1])
            filter (
                (d != null and d <= @nearbyMaxDistance) or x.${f(Story::person)} == @personKey or first(
                    for group, myMember in outbound @person graph `${Member::class.graph()}`
                        filter myMember.${f(Member::gone)} != true
                        for friend, member in inbound group graph `${Member::class.graph()}`
                            filter member.${f(Member::gone)} != true
                                and friend._key == x.${f(Story::person)}
                            limit 1
                            return true
                ) == true
            )
            sort x.${f(Story::publishDate)} desc, x.${f(Story::createdAt)} desc
            limit @offset, @limit
            return ${withAuthors("x")}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "personKey" to person,
        "geo" to geo,
        "nearbyMaxDistance" to nearbyMaxDistance,
        "offset" to offset,
        "limit" to limit
    )
)

/**
 * @geo The geolocation bias
 * @person The current user
 */
fun Db.countStories(geo: List<Double>, person: String, nearbyMaxDistance: Double, after: Instant?) = query(
    Int::class,
    """
        return count(
            for x in ${Story::class.collection()}
                filter x.${f(Story::published)} == true
                    and (@after == null or x.${f(Story::publishDate)} > @after)
                let d = x.${f(Story::geo)} == null ? null : distance(x.${f(Story::geo)}[0], x.${f(Story::geo)}[1], @geo[0], @geo[1])
                filter (
                    (d != null and d <= @nearbyMaxDistance) or x.${f(Story::person)} == @personKey or first(
                        for group in outbound @person graph `${Member::class.graph()}`
                            for friend, member in inbound group graph `${Member::class.graph()}`
                                filter member.${f(Member::gone)} != true
                                    and friend._key == x.${f(Story::person)}
                                limit 1
                                return true
                    ) == true
                )
                return true
        )
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "personKey" to person,
        "geo" to geo,
        "nearbyMaxDistance" to nearbyMaxDistance,
        "after" to after,
    )
).first()!!

/**
 * @person The person to fetch equipped cards for
 */
fun Db.equippedCardsOfPerson(person: String, me: String?) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
                and x.${f(Card::equipped)} == true
                and x.${f(Card::active)} == true
            sort x.${f(Card::createdAt)} desc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter ((@me != null && card.${f(Card::person)} == @me) or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person,
        "me" to me,
    )
)

/**
 * @person The current user
 */
fun Db.collaborationsOfPerson(person: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
                or @person in x.${f(Card::collaborators)}
            sort x.${f(Card::createdAt)} desc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

/**
 * @person The current user
 * @search Optionally filter cards
 */
fun Db.savesOfPerson(person: String, search: String? = null) = query(
    SaveAndCard::class,
    """
        for save in `${Save::class.collection()}`
            for x in `${Card::class.collection()}`
            filter x._key == save.${f(Save::card)}
                and save.${f(Save::person)} == @person
                and (@search == null or contains(lower(x.${f(Card::name)}), @search) or contains(lower(x.${f(Card::location)}), @search) or contains(lower(x.${f(Card::conversation)}), @search))
            sort save.${f(Save::createdAt)} desc
            return {
                save: save,
                card: merge(
                    x,
                    {
                        cardCount: count(for card in `${Card::class.collection()}` filter card.${f(Card::active)} == true && card.${f(Card::parent)} == x._key return card)
                    }
                )
            }
    """.trimIndent(),
    mapOf(
        "person" to person,
        "search" to search?.lowercase()
    )
)

/**
 * @person The current user
 * @card The card to save
 */
fun Db.saveCard(person: String, card: String) = one(
    Save::class,
    """
            upsert { ${f(Save::person)}: @person, ${f(Save::card)}: @card }
                insert { ${f(Save::person)}: @person, ${f(Save::card)}: @card, ${f(Save::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { ${f(Save::person)}: @person, ${f(Save::card)}: @card}
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "person" to person,
        "card" to card
    )
)

/**
 * @person The current user
 * @card The card to unsave
 */
fun Db.unsaveCard(person: String, card: String) = query(
    Save::class,
    """
            for x in `${Save::class.collection()}`
                filter x.${f(Save::person)} == @person
                    and x.${f(Save::card)} == @card
                remove x in `${Save::class.collection()}`
        """,
    mapOf(
        "person" to person,
        "card" to card
    )
)

/**
 * @person The current user
 * @geo The current user's geolocation
 */
fun Db.updateEquippedCards(person: String, geo: List<Double>) = query(
    Card::class,
    """
        for x in `${Card::class.collection()}`
            filter x.${f(Card::person)} == @person
                and x.${f(Card::equipped)} == true
            update { _key: x._key, ${f(Card::geo)}: @geo } in `${Card::class.collection()}`
    """.trimIndent(),
    mapOf(
        "geo" to geo,
        "person" to person
    )
)

/**
 * @person The current user
 * @geo The geolocation bias
 * @search Optionally filter cards
 * @nearbyMaxDistance Optionally include cards nearby that may not be of the current user's friends
 * @offset Page offset
 * @limit Page size
 */
fun Db.explore(person: String?, geo: List<Double>, search: String? = null, nearbyMaxDistance: Double = 0.0, offset: Int = 0, limit: Int = 20) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::active)} == true
                and (x.${f(Card::parent)} == null or @search != null) // When searching, include cards inside other cards
                and (x.${f(Card::geo)} != null or @search != null) // When searching, include cards inside other cards
                and x.${f(Card::offline)} != true
                ${if (person == null) "" else "and (x.${f(Card::person)} != @personKey or x.${f(Card::equipped)} != true)"}
                and (
                    @search == null 
                        or contains(lower(x.${f(Card::name)}), @search)
                        or contains(lower(x.${f(Card::location)}), @search)
                        or contains(lower(x.${f(Card::conversation)}), @search)
                        or (is_array(x.${f(Card::categories)}) and first(for c in (x.${f(Card::categories)} || []) filter contains(lower(c), @search) return true) == true)
                )
            let d = x.${f(Card::geo)} == null ? null : distance(x.${f(Card::geo)}[0], x.${f(Card::geo)}[1], @geo[0], @geo[1])
            filter (
                    (d != null and d <= @nearbyMaxDistance) ${if (person == null) "" else orIsFriendProfileCard()}
            )
            sort d == null, d
            limit @offset, @limit
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter card.${f(Card::active)} == true && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    buildMap {
        if (person != null) {
            put("personKey", person.asKey())
            put("person", person.asId(Person::class))
        }
        put("geo", geo)
        put("search", search?.trim()?.lowercase())
        put("nearbyMaxDistance", nearbyMaxDistance)
        put("offset", offset)
        put("limit", limit)
    }
)

fun Db.orIsFriendProfileCard() = """or (x.${f(Card::equipped)} == true and first(
for group, myMember in outbound @person graph `${Member::class.graph()}`
    filter myMember.${f(Member::gone)} != true
    for friend, member in inbound group graph `${Member::class.graph()}`
        filter member.${f(Member::gone)} != true
            and friend._key == x.${f(Card::person)}
        limit 1
        return true
) == true)"""

fun Db.cards(geo: List<Double>, search: String? = null, offset: Int = 0, limit: Int = 20) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::active)} == true
                and x.${f(Card::parent)} == null
                and x.${f(Card::geo)} != null
                and x.${f(Card::offline)} != true
                and (@search == null or contains(lower(x.${f(Card::name)}), @search) or contains(lower(x.${f(Card::location)}), @search) or contains(lower(x.${f(Card::conversation)}), @search))
            sort distance(x.${f(Card::geo)}[0], x.${f(Card::geo)}[1], @geo[0], @geo[1])
            limit @offset, @limit
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter card.${f(Card::active)} == true && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "geo" to geo,
        "search" to search?.lowercase(),
        "offset" to offset,
        "limit" to limit
    )
)

/**
 * @card The card
 * @person The current user
 */
fun Db.cardsOfCard(card: String, person: String?) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::parent)} == @card
                and (x.${f(Card::active)} == true or x.${f(Card::person)} == @person)
           sort x.${f(Card::name)} asc
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return true)
                }
            )
    """.trimIndent(),
    mapOf(
        "card" to card,
        "person" to person
    )
)

/**
 * @card The card to fetch all cards off
 */
fun Db.allCardsOfCard(card: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::parent)} == @card
            return x
    """.trimIndent(),
    mapOf(
        "card" to card
    )
)

/**
 * @people The list of people to fetch
 */
fun Db.people(people: List<String>) = list(
    Person::class,
    """
        for x in @@collection
            filter x._key in @people
            return x
    """.trimIndent(),
    mapOf(
        "people" to people
    )
)

/**
 * @person the person to fetch a profile for
 */
fun Db.profile(person: String) = one(
    Profile::class,
    """
    upsert { ${f(Profile::person)}: @person }
        insert { ${f(Profile::person)}: @person, ${f(Profile::createdAt)}: DATE_ISO8601(DATE_NOW()) }
        update { }
        in @@collection
        return NEW || OLD
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)!!

/**
 * @url the url to fetch a profile for
 */
fun Db.profileByUrl(url: String) = one(
    Profile::class,
    """
    for x in @@collection
        filter x.${f(Profile::url)} == @url
        limit 1
        return x
    """.trimIndent(),
    mapOf(
        "url" to url
    )
)

/**
 * @person The current user
 */
fun Db.presenceOfPerson(person: String) = one(
    Presence::class,
    """
    upsert { ${f(Presence::person)}: @person }
        insert {
            ${f(Presence::person)}: @person,
            ${f(Presence::createdAt)}: DATE_ISO8601(DATE_NOW())
        }
        update {}
        in @@collection
        return NEW || OLD
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)!!

private fun Db.groupExtended(groupVar: String = "group") = """{
    $groupVar,
    members: (
        for person, member in inbound $groupVar graph `${Member::class.graph()}`
            filter member.${f(Member::gone)} != true
            sort member.${f(Member::seen)} desc
            return {
                person,
                member
            }
    ),
    latestMessage: first(
        for message in `${Message::class.collection()}`
            filter message.${f(Message::group)} == group._key
            sort message.${f(Message::createdAt)} desc
            limit 1
            return message
    )
}"""

/**
 * @person The current user
 */
fun Db.groups(person: String) = query(
    GroupExtended::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::hide)} != true
                and edge.${f(Member::gone)} != true
            sort group.${f(Group::seen)} desc
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class)
    )
)

/**
 * @person The current user
 * @group The group to fetch
 */
fun Db.group(person: String, group: String) = query(
    GroupExtended::class,
    """
        for group in outbound @person graph `${Member::class.graph()}`
            filter group._key == @group
            limit 1
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "group" to group,
    )
).first()!!

/**
 * @me The current user
 */
fun Db.hiddenGroups(me: String) = query(
    GroupExtended::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::hide)} == true
                and edge.${f(Member::gone)} != true
            sort group.${f(Group::seen)} desc
            return ${groupExtended()}
    """.trimIndent(),
    mapOf(
        "person" to me.asId(Person::class)
    )
)

/**
 * @person The current user
 * @group The group to fetch
 */
fun Db.groups(person: String, groups: List<String>) = list(
    Group::class,
    """
        for x in outbound @person graph `${Member::class.graph()}`
            filter x._key in @groups
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "groups" to groups,
    )
)

/**
 * The current user
 */
fun Db.transferOfPerson(person: String) = one(
    Transfer::class,
    """
        for x in @@collection
            filter x.${f(Transfer::person)} == @person
            return x
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

/**
 * @code the code of the transfer to fetch
 */
// todo filter 5 min
fun Db.transferWithCode(code: String) = one(
    Transfer::class,
    """
        for x in @@collection
            filter x.${f(Transfer::code)} == @code
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "code" to code
    )
)

/**
 * @group The group to fetch devices for
 */
fun Db.memberDevices(group: String) = query(
    MemberDevice::class,
    """
        for member in `${Member::class.collection()}`
            filter member._to == @group and member.${f(Member::gone)} != true
            return {
                member,
                devices: (
                    for device in `${Device::class.collection()}`
                        filter device.${f(Device::person)} == member._from
                        return device
                )
            }
    """.trimIndent(),
    mapOf(
        "group" to group.asId(Group::class)
    )
)
/**
 * @people The people to fetch devices for
 */
fun Db.peopleDevices(people: List<String>) = list(
    Device::class,
    """
        for device in @@collection
            filter device.${f(Device::person)} in @people
            return device
    """.trimIndent(),
    mapOf(
        "people" to people.map { it.asId(Person::class).also {
            println("Notifying person $it")
        } }
    )
).also {
    println("Notifying ${it.size} device(s)")
}

/**
 * @person The person in the group
 * @group The group
 */
fun Db.member(person: String, group: String) = one(
    Member::class,
    """
        for x in @@collection
            filter x._from == @person
                and x._to == @group
                and x.${f(Member::gone)} != true
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "group" to group.asId(Group::class),
    )
)

/**
 * @people The list of all people in the group
 *
 * @return The most recently active group with all these people, or null
 */
fun Db.group(people: List<String>) = one(
    Group::class,
    """
        for x in @@collection
            let members = (
                for person, edge in inbound x graph `${Member::class.graph()}`
                    filter edge.${f(Member::gone)} != true
                    return edge._from
            )
            filter @people all in members
                and count(@people) == count(members)
            sort x.${f(Group::seen)} desc
            limit 1
            return x
    """.trimIndent(),
    mapOf(
        "people" to people.map { it.asId(Person::class) }
    )
)

fun Db.messages(group: String, before: Instant? = null, limit: Int = 20) = list(
    Message::class,
    """
        for x in @@collection
            filter x.${f(Message::group)} == @group
            and (@before == null or x.${f(Message::createdAt)} <= @before)
            sort x.${f(Message::createdAt)} desc
            limit @limit
            return x
    """.trimIndent(),
    mapOf(
        "group" to group,
        "before" to before,
        "limit" to limit
    )
)

fun Db.updateDevice(person: String, type: DeviceType, token: String) = one(
    Device::class,
        """
            upsert { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                insert { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person, ${f(Person::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person }
                in @@collection
                return NEW || OLD
        """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "type" to type,
        "token" to token
    )
)

fun Db.deleteDevice(type: DeviceType, token: String) = query(
    Device::class,
    """
        for x in ${Device::class.collection()}
            filter x.${f(Device::type)} == @type
                and x.${f(Device::token)} == @token
            remove x in ${Device::class.collection()}
    """.trimIndent(),
    mapOf(
        "type" to type,
        "token" to token
    )
)

fun Db.device(type: DeviceType, token: String) = one(
    Device::class,
        """
            upsert { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                insert { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Person::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "type" to type,
        "token" to token
    )
)!!

fun Db.withStickers(stickerPackVal: String) = """
    merge(
        $stickerPackVal,
        {
            ${f(StickerPack::stickers)}: (
                for sticker in ${Sticker::class.collection()}
                    filter sticker.${f(Sticker::pack)} == $stickerPackVal._key
                    sort sticker.${f(Sticker::name)}, sticker.${f(Sticker::createdAt)}
                    return sticker
                )
        }
    )
""".trimIndent()

fun Db.myStickerPacks(person: String) = list(
    StickerPack::class,
    """
        for pack in @@collection
            filter pack.${f(StickerPack::person)} == @person
            sort pack.${f(StickerPack::createdAt)} desc
            return ${withStickers("pack")}
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.stickerPacks(person: String)  = list(
    StickerPack::class,
    """
        for pack, save in outbound @person graph `${StickerPackSave::class.graph()}`
            sort save.${f(StickerPackSave::createdAt)} desc
            return ${withStickers("pack")}
    """,
    mapOf(
        "person" to person.asId(Person::class)
    )
)

fun Db.stickerPackWithStickers(stickerPack: String) = one(
    StickerPack::class,
    """
        for pack in @@collection
            filter pack._key == @pack
            return ${withStickers("pack")}
    """,
    mapOf(
        "pack" to stickerPack
    )
)

fun Db.saveStickerPack(person: String, stickerPack: String) = one(
    StickerPackSave::class,
    """
            upsert { _from: @person, _to: @pack }
                insert { _from: @person, _to: @pack, ${f(StickerPackSave::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { _from: @person, _to: @pack }
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "person" to person.asId(Person::class),
        "pack" to stickerPack.asId(StickerPack::class)
    )
)

fun Db.unsaveStickerPack(person: String, stickerPack: String) = query(
    StickerPackSave::class,
    """
            for x in `${StickerPackSave::class.collection()}`
                filter x._from == @person
                    and x._to == @pack
                remove x in `${StickerPackSave::class.collection()}`
        """,
    mapOf(
        "person" to person.asId(Person::class),
        "pack" to stickerPack.asId(StickerPack::class)
    )
)

fun Db.stickers(stickerPack: String) = list(
    Sticker::class,
    """
        for x in @@collection
            filter x.${f(Sticker::pack)} == @pack
            return x
    """.trimIndent(),
    mapOf(
        "pack" to stickerPack
    )
)


class MemberDevice(
    var member: Member? = null,
    var devices: List<Device>? = null
)

class GroupExtended(
    var group: Group? = null,
    var members: List<MemberAndPerson>? = null,
    var latestMessage: Message? = null
)

class MemberAndPerson(
    var person: Person? = null,
    var member: Member? = null
)

class SaveAndCard(
    var save: Person? = null,
    var card: Card? = null
)
