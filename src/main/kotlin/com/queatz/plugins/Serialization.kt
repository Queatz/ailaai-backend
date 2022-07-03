package com.queatz.plugins

import com.arangodb.velocypack.internal.util.DateUtil
import com.google.gson.*
import io.ktor.http.parsing.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.lang.reflect.Type
import java.util.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(Instant::class.java, InstantTypeConverter())
        }
    }
}

class InstantTypeConverter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(
        src: Instant,
        srcType: Type,
        context: JsonSerializationContext
    ) = JsonPrimitive(DateUtil.format(Date.from(src.toJavaInstant())))

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ) = try {
        DateUtil.parse(json.asString).toInstant().toKotlinInstant()
    } catch (e: ParseException) {
        null
    }
}
