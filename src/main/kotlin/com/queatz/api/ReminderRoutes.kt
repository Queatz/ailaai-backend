package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.toInstant

fun Route.reminderRoutes() {
    authenticate {
        get("/reminders") {
            respond {
                db.reminders(me.id!!)
            }
        }

        get("/occurrences") {
            respond {
                val from = call.parameters["from"]?.toInstant() ?: return@respond HttpStatusCode.BadRequest.description("Missing 'from' parameter")
                val to = call.parameters["to"]?.toInstant() ?: return@respond HttpStatusCode.BadRequest.description("Missing 'to' parameter")

                // todo calc all reminders and add any occurrences.date
                emptyList<ReminderOccurrence>()
            }
        }

        post("/reminders") {
            respond {
                val new = call.receive<Reminder>()

//                todo val myMember = db.member(group.id!!, me.id!!) ?: return@forEach

                db.insert(
                    Reminder(
                        person = me.id!!,
                        groups = new.groups, // todo check that I'm a member of each group
                        attachment = new.attachment,
                        title = new.title,
                        note = new.note,
                        start = new.start,
                        end = new.end,
                        schedule = new.schedule
                    )
                )
            }
        }

        get("/reminders/{id}") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (reminder.person == me.id) {
                    reminder
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }

        post("/reminders/{id}") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (reminder.person == me.id) {
                    val update = call.receive<Reminder>()

                    if (update.title != null) {
                        reminder.title = update.title
                    }

                    if (update.note != null) {
                        reminder.note = update.note
                    }

                    if (update.groups != null) {
                        reminder.groups = update.groups
                    }

                    if (update.attachment != null) {
                        reminder.attachment = update.attachment
                    }

                    if (update.start != null) {
                        reminder.start = update.start
                    }

                    // TODO can be deleted
                    if (update.end != null) {
                        reminder.end = update.end
                    }

                    // TODO can be deleted
                    if (update.schedule != null) {
                        reminder.schedule = update.schedule
                    }

                    db.update(reminder)
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }

        post("/reminders/{id}/occurrences/{occurrence}") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (reminder.person != me.id) {
                    return@respond HttpStatusCode.NotFound
                }

                val at = parameter("occurrence").toInstant()
                val occurrence = db.occurrence(reminder.id!!, at) ?: ReminderOccurrence(
                    reminder = reminder.id!!,
                    occurrence = at
                ).let(db::insert)

                val occurrenceUpdate = call.receive<ReminderOccurrence>()

                if (occurrenceUpdate.date != null) {
                    occurrence.date = occurrenceUpdate.date
                }
                if (occurrenceUpdate.note != null) {
                    occurrence.note = occurrenceUpdate.note
                }
                if (occurrenceUpdate.done != null) {
                    occurrence.done = occurrenceUpdate.done
                }

                db.update(occurrence)
            }
        }

        post("/reminders/{id}/delete") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (reminder.person != me.id) {
                    return@respond HttpStatusCode.NotFound
                }

                db.deleteReminderOccurrences(reminder.id!!)
                db.delete(reminder)

                HttpStatusCode.NoContent
            }
        }

        post("/reminders/{id}/occurrences/{date}/delete") {
            respond {
                val reminder = db.document(Reminder::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (reminder.person != me.id) {
                    return@respond HttpStatusCode.NotFound
                }

                db.upsertReminderOccurrenceGone(reminder.id!!, parameter("date").toInstant(), true)

                HttpStatusCode.NoContent
            }
        }
    }
}
