package com.queatz.db

val Db.totalPeople
    get() = query(
        Int::class,
        """
            return count(`${Person::class.collection()}`)
        """
    ).first()!!

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

fun Db.cardsOfPerson(person: String) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::person)} == @person
            sort x.${f(Card::createdAt)} desc
            return x
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.cards(geo: List<Double>, search: String? = null, offset: Int = 0, limit: Int = 20) = list(
    Card::class,
    """
        for x in @@collection
            filter x.${f(Card::active)} == true
                and (@search == null or contains(lower(x.${f(Card::conversation)}), @search))
            sort distance(x.${f(Card::geo)}[0], x.${f(Card::geo)}[1], @geo[0], @geo[1])
            limit @offset, @limit
            return x
    """.trimIndent(),
    mapOf(
        "geo" to geo,
        "search" to search?.lowercase(),
        "offset" to offset,
        "limit" to limit
    )
)

fun Db.groups(person: String) = query(
    GroupExtended::class,
    """
        for group in outbound @person graph `${Member::class.graph()}`
            sort group.${f(Group::seen)} desc
            return {
                group,
                members: (
                    for person, member in inbound group graph `${Member::class.graph()}`
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

fun Db.group(person: String, group: String) = query(
    GroupExtended::class,
    """
        for group in outbound @person graph `${Member::class.graph()}`
            filter group._key == @group
            return {
                group,
                members: (
                    for person, member in inbound group graph `${Member::class.graph()}`
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

fun Db.memberDevices(group: String) = query(
    MemberDevice::class,
    """
        for member in `${Member::class.collection()}`
            filter member._to == @group
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

fun Db.member(person: String, group: String) = one(
    Member::class,
    """
        for x in @@collection
            filter x._from == @person and x._to == @group
            return x
    """.trimIndent(),
    mapOf(
        "person" to person.asId(Person::class),
        "group" to group.asId(Group::class),
    )
)

fun Db.group(people: List<String>) = one(
    Group::class,
    """
        for x in @@collection
            filter @people all in (
                for person, edge in inbound x graph `${Member::class.graph()}` return edge._from
            )
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
        """,
    mapOf(
        "person" to person.asId(Person::class),
        "type" to type,
        "token" to token
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
