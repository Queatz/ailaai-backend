package com.queatz.db

val Db.totalPeople
    get() = query(
        Int::class,
        """
            return count(`${Person::class.collection()}`)
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
 * Find people matching @name that are not connected with @person sorted by distance from @geo.
 */
fun Db.peopleWithName(person: String, name: String, geo: List<Double>? = null) = list(
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
            limit 20
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "name" to name.lowercase(),
        "geo" to geo
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
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return card)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person
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
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return card)
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
fun Db.explore(person: String, geo: List<Double>, search: String? = null, nearbyMaxDistance: Int = 0, offset: Int = 0, limit: Int = 20) = list(
    Card::class,
    """
        for x in @@collection
            let d = x.${f(Card::geo)} == null ? null : distance(x.${f(Card::geo)}[0], x.${f(Card::geo)}[1], @geo[0], @geo[1])
            filter x.${f(Card::active)} == true
                and (x.${f(Card::parent)} == null or @search != null) // When searching, include cards inside other cards
                and (x.${f(Card::geo)} != null or @search != null) // When searching, include cards inside other cards
                and x.${f(Card::offline)} != true
                and (@search == null or contains(lower(x.${f(Card::name)}), @search) or contains(lower(x.${f(Card::location)}), @search) or contains(lower(x.${f(Card::conversation)}), @search))
                and ((d != null and d <= @nearbyMaxDistance) or (x.${f(Card::equipped)} == true and first(
                    for group in `${Group::class.collection()}`
                        for person, member in inbound group graph `${Member::class.graph()}`
                                filter member.${f(Member::gone)} != true and person._key == @person
                            for friend, member2 in inbound group graph `${Member::class.graph()}`
                                    filter member2.${f(Member::gone)} != true and friend._key == x.${f(Card::person)}
                                limit 1
                                return true
                ) == true))
            sort d
            sort d == null
            limit @offset, @limit
            return merge(
                x,
                {
                    cardCount: count(for card in @@collection filter card.${f(Card::active)} == true && card.${f(Card::parent)} == x._key return card)
                }
            )
    """.trimIndent(),
    mapOf(
        "person" to person,
        "geo" to geo,
        "search" to search?.trim()?.lowercase(),
        "nearbyMaxDistance" to nearbyMaxDistance,
        "offset" to offset,
        "limit" to limit
    )
)

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
                    cardCount: count(for card in @@collection filter card.${f(Card::active)} == true && card.${f(Card::parent)} == x._key return card)
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
                    cardCount: count(for card in @@collection filter (card.${f(Card::person)} == @person or card.${f(Card::active)} == true) && card.${f(Card::parent)} == x._key return card)
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
 * @person The current user
 */
fun Db.groups(person: String) = query(
    GroupExtended::class,
    """
        for group, edge in outbound @person graph `${Member::class.graph()}`
            filter edge.${f(Member::hide)} != true
                and edge.${f(Member::gone)} != true
            sort group.${f(Group::seen)} desc
            return {
                group,
                members: (
                    for person, member in inbound group graph `${Member::class.graph()}`
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
            }
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
            return {
                group,
                members: (
                    for person, member in inbound group graph `${Member::class.graph()}`
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
            }
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "group" to group,
    )
).firstOrNull()

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

fun Db.messages(group: String, offset: Int = 0, limit: Int = 20) = list(
    Message::class,
    """
        for x in @@collection
            filter x.${f(Message::group)} == @group
            sort x.${f(Message::createdAt)} desc
            limit @offset, @limit
            return x
    """.trimIndent(),
    mapOf(
        "group" to group,
        "offset" to offset,
        "limit" to limit
    )
)

fun Db.updateDevice(person: String, type: DeviceType, token: String) = one(
    Device::class,
        """
            upsert { ${f(Device::type)}: @type, ${f(Device::token)}: @token }
                insert { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person, ${f(Person::createdAt)}: DATE_ISO8601(DATE_NOW()) }
                update { ${f(Device::type)}: @type, ${f(Device::token)}: @token, ${f(Device::person)}: @person}
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
                update { ${f(Device::type)}: @type, ${f(Device::token)}: @token}
                in @@collection
                return NEW || OLD
        """,
    mapOf(
        "type" to type,
        "token" to token
    )
)!!

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
