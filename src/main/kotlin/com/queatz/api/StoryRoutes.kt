package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.*
import com.queatz.receiveFile
import com.queatz.receiveFiles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock

fun Route.storyRoutes() {
    authenticate(optional = true) {
        get("/stories/{id}") {
            respond {
                db.story(parameter("id"))
                    //?.takeIf { it.published == true || it.person == meOrNull?.id } // todo, authorize?
                    ?: HttpStatusCode.NotFound
            }
        }
    }

    authenticate {
        get("/stories") {
            respond {
                val geo = call.parameters["geo"]!!.split(",").map { it.toDouble() }

                if (geo.size != 2) {
                    return@respond HttpStatusCode.BadRequest.description("'geo' must be an array of size 2")
                }

                db.stories(
                    geo,
                    me.id!!,
                    500_000.0,
                    call.parameters["offset"]?.toInt() ?: 0,
                    call.parameters["limit"]?.toInt() ?: 20
                )
            }
        }

        post("/stories") {
            respond {
                val title = call.receive<Story>().title
                db.insert(Story(title = title, person = me.id!!))
            }
        }

        post("/stories/{id}") {
            respond {
                val story = db.document(Story::class, parameter("id"))
                    ?.takeIf { it.person == me.id } // authorize
                    ?: return@respond HttpStatusCode.NotFound

                if (story.published == true) {
                    return@respond HttpStatusCode.BadRequest.description("Story is published")
                }

                val update = call.receive<Story>()

                val numbers = "\\d".toRegex()

                fun String.uniqueInDb(id: String) = if (
                    all { numbers.matches("$it") }
                ) {
                    "story-$this"
                } else {
                    this
                }.let { url ->
                    "$url-$id"
                }

                // Publishing can't be combined with any other update
                if (story.published != true && update.published == true && story.publishDate == null) {
                    story.published = true
                    story.publishDate = Clock.System.now()
                    story.url = story.title!!.urlize().uniqueInDb(story.id!!)

                    // Share to groups
                    db.storyDraft(me.id!!, story.id!!)?.groupDetails?.let { groups ->
                        val attachment = json.toJson(StoryAttachment(story.id!!))
                        groups.forEach { group ->
                            db.insert(Message(group.id!!, me.id, text = null, attachment))

                            notify.message(
                                group = group,
                                from = me,
                                message = Message(text = null, attachment = attachment)
                            )
                        }
                    }
                } else if (story.published != true) {
                    if (update.title != null) {
                        story.title = update.title
                    }

                    if (update.content != null) {
                        story.content = update.content
                    }

                    if (update.geo != null) {
                        story.geo = update.geo?.takeIf { it.isNotEmpty() }
                    }
                }

                db.update(story)
            }
        }

        post("/stories/{id}/photos") {
            respond {
                val storyId = parameter("id")
                var result: List<String>? = null
                call.receiveFiles("photo", "story-${storyId}") { photosUrls, _ ->
                    result = photosUrls
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }

        post("/stories/{id}/audio") {
            respond {
                val storyId = parameter("id")
                var result: String? = null
                call.receiveFile("audio", "story-${storyId}") { audioUrl, _ ->
                    result = audioUrl
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }

        post("/stories/{id}/delete") {
            respond {
                val story = db.document(Story::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
                if (story.person == me.id) {
                    db.delete(story)
                    HttpStatusCode.NoContent
                } else {
                    HttpStatusCode.Forbidden
                }
            }
        }

        get("/stories/{id}/draft") {
            respond {
                db.storyDraft(me.id!!, parameter("id"))?.also {
                    it.groups?.let { groups ->
                        it.groupDetails = db.groups(me.id!!, groups)
                    }
                } ?: HttpStatusCode.NotFound
            }
        }

        post("/stories/{id}/draft") {
            respond {
                val draft = db.storyDraft(me.id!!, parameter("id")) ?: StoryDraft(
                    story = parameter("id")
                ).let {
                    db.insert(it)
                }

                val update = call.receive<StoryDraft>()

                if (update.groups != null) {
                    draft.groups = update.groups
                }

                db.update(draft)
            }
        }
    }
}

fun String.urlize() = "\\s+".toRegex().replace(lowercase(), "-").encodeURLPathPart()
