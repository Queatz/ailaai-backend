package com.queatz.db

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
fun Db.savedCardsOfPerson(person: String, search: String? = null, offset: Int = 0, limit: Int = 20) = query(
    SaveAndCard::class,
    """
        for save in `${Save::class.collection()}`
            for x in `${Card::class.collection()}`
            filter x._key == save.${f(Save::card)}
                and save.${f(Save::person)} == @person
                and (@search == null or contains(lower(x.${f(Card::name)}), @search) or contains(lower(x.${f(Card::location)}), @search) or contains(lower(x.${f(Card::conversation)}), @search))
            sort save.${f(Save::createdAt)} desc
            limit @offset, @limit
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
        "search" to search?.lowercase(),
        "offset" to offset,
        "limit" to limit
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
fun Db.explore(
    person: String?,
    geo: List<Double>,
    search: String? = null,
    nearbyMaxDistance: Double = 0.0,
    offset: Int = 0,
    limit: Int = 20,
    public: Boolean = false
) = list(
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
                ${if (person == null || public) "d != null and d <= @nearbyMaxDistance" else isFriendCard(false)}
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
            if (!public) {
                put("person", person.asId(Person::class))
            }
        }
        put("geo", geo)
        put("search", search?.trim()?.lowercase())
        if (person == null || public) {
            put("nearbyMaxDistance", nearbyMaxDistance)
        }
        put("offset", offset)
        put("limit", limit)
    }
)

fun Db.isFriendCard(onlyProfile: Boolean) = """(${if (onlyProfile) "x.${f(Card::equipped)} == true and " else ""}first(
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
