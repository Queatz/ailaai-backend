package com.queatz.db

import com.arangodb.entity.From
import com.arangodb.entity.Key
import com.arangodb.entity.To
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Instant

class Person(
    var name: String? = null,
    var photo: String? = null,
    var geo: List<Double>? = null,
    var inviter: String? = null,
    var seen: Instant? = null,
    var language: String? = null,
    var source: PersonSource? = null
) : Model()

class Settings(
    var person: String? = null,
    var language: String? = null
) : Model()

class Invite(
    var person: String? = null,
    var code: String? = null
) : Model()

class Save(
    var person: String? = null,
    var card: String? = null
) : Model()

class Transfer(
    var person: String? = null,
    var code: String? = null
) : Model()

class Card(
    var person: String? = null,
    var parent: String? = null,
    var name: String? = null,
    var photo: String? = null,
    var location: String? = null,
    var collaborators: List<String>? = null,
    var equipped: Boolean? = null,
    var offline: Boolean? = null,
    var geo: List<Double>? = null,
    var conversation: String? = null,
    var active: Boolean? = null,
    var cardCount: Int? = null
) : Model()

class Group(
    var name: String? = null,
    var seen: Instant? = null,
    var description: String? = null
) : Model()

class Member(
    var seen: Instant? = null,
    var hide: Boolean? = null,
    var gone: Boolean? = null,
    var host: Boolean? = null,
    from: String? = null,
    to: String? = null
) : Edge(from, to)

class Message(
    var group: String? = null,
    var member: String? = null,
    var text: String? = null,
    var attachment: String? = null,
) : Model()

class Device(
    var person: String? = null,
    var type: DeviceType? = null,
    var token: String? = null
) : Model()

enum class DeviceType {
    Hms,
    Gms,
    Web
}

enum class PersonSource {
    Web
}

open class Model(
    @Key
    @SerializedName(value = "id", alternate = ["_id"])
    var id: String? = null,

    var createdAt: Instant? = null
)

open class Edge(
    @From
    @SerializedName(value = "from", alternate = ["_from"])
    var from: String? = null,

    @To
    @SerializedName(value = "to", alternate = ["_to"])
    var to: String? = null
) : Model()
