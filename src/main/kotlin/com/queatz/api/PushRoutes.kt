package com.queatz.api

import com.queatz.PushData
import com.queatz.db.DeviceType
import com.queatz.db.device
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.json
import com.queatz.plugins.push
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import java.util.logging.Logger

private suspend fun ApplicationCall.respondSse(events: Flow<PushData>) {
    response.cacheControl(CacheControl.NoCache(null))
    response.header("X-Accel-Buffering", "no")
    respondBytesWriter(contentType = ContentType.Text.EventStream) {
        Logger.getAnonymousLogger().info("Collecting events for web")
        events.collect { event ->
            Logger.getAnonymousLogger().info("Sending to web: ${event.data}")
//            if (event.id != null) {
//                writeStringUtf8("id: ${event.id}\n")
//            }
//            if (event.event != null) {
//                writeStringUtf8("event: ${event.event}\n")
//            }
            for (dataLine in json.encodeToString(event).lines()) {
                writeStringUtf8("data: $dataLine\n")
            }
            writeStringUtf8("\n")
            flush()
        }
    }
}

fun Route.pushRoutes() {
    get("/push/{token}") {
        val token = parameter("token")
        val device = db.device(DeviceType.Web, token)
        call.respondSse(push.flow(device.id!!))
    }
}
