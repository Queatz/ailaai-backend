package com.queatz.db

import kotlinx.datetime.Instant

fun Db.reminders(person: String) = list(
    Reminder::class,
    """
        for x in @@collection
            filter x.${f(Reminder::person)} == @person
            return x
    """.trimIndent(),
    mapOf(
        "person" to person
    )
)

fun Db.occurrence(reminder: String, occurrence: Instant) = one(
    ReminderOccurrence::class,
    """
        for x in @@collection
            filter x.${f(ReminderOccurrence::reminder)} == @reminder
                and x.${f(ReminderOccurrence::occurrence)} == @occurrence
            return x
    """.trimIndent(),
    mapOf(
        "reminder" to reminder,
        "occurrence" to occurrence
    )
)

fun Db.deleteReminderOccurrences(reminder: String) = query(
    ReminderOccurrence::class,
    """
        for x in ${ReminderOccurrence::class.collection()}
            filter x.${f(ReminderOccurrence::reminder)} == @reminder
            remove x in ${ReminderOccurrence::class.collection()}
    """.trimIndent(),
    mapOf(
        "reminder" to reminder
    )
)

fun Db.upsertReminderOccurrenceGone(reminder: String, occurrence: Instant, gone: Boolean) = query(
    ReminderOccurrence::class,
    """
        upsert { ${f(ReminderOccurrence::reminder)}: @reminder }
        insert {
            ${f(ReminderOccurrence::occurrence)}: @occurrence,
            ${f(ReminderOccurrence::gone)}: true,
            ${f(ReminderOccurrence::createdAt)}: DATE_ISO8601(DATE_NOW())
        }
        update {
            ${f(ReminderOccurrence::gone)}: @gone
        }
        in ${ReminderOccurrence::class.collection()}
        return NEW
    """.trimIndent(),
    mapOf(
        "reminder" to reminder,
        "occurrence" to occurrence,
        "gone" to gone
    )
)

fun Db.upsertReminderOccurrenceDone(reminder: String, occurrence: Instant, done: Boolean) = query(
    ReminderOccurrence::class,
    """
        upsert { ${f(ReminderOccurrence::reminder)}: @reminder }
        insert {
            ${f(ReminderOccurrence::occurrence)}: @occurrence,
            ${f(ReminderOccurrence::gone)}: true,
            ${f(ReminderOccurrence::createdAt)}: DATE_ISO8601(DATE_NOW())
        }
        update {
            ${f(ReminderOccurrence::gone)}: true
        }
        in @@collection
        return NEW
    """.trimIndent(),
    mapOf(
        "reminder" to reminder,
        "occurrence" to occurrence,
        "done" to done
    )
)
