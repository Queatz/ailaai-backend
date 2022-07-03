package com.queatz.db

val Db.totalPeople
    get() = query(
        Int::class,
        """
            return count('${Person::class.collection()}')
        """
    ).first()!!

fun Db.invite(code: String) = one(
    Invite::class,
    """
        for x in @@collection
            filter x.${f(Invite::code)} = @code
            return x
    """.trimIndent(),
    mapOf(
        "code" to code
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
