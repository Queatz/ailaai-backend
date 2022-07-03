package com.queatz.db

import com.arangodb.entity.From
import com.arangodb.entity.Id
import com.arangodb.entity.To
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Instant

class Person(
    var name: String? = null,
    var photo: String? = null,
    var inviter: String? = null
) : Model()

class Settings(
    var person: String? = null,
    var language: String? = null
) : Model()

class Invite(
    var person: String? = null,
    var code: String? = null
) : Model()

class Card(
    var person: String? = null,
    var name: String? = null,
    var location: String? = null,
    var geo: List<Double>? = null,
    var conversation: String? = null,
    var active: Boolean? = null
) : Model()

class Group : Model()

class Member(
    val group: String? = null,
    val person: String? = null
) : Model()

class Message(
    val group: String? = null,
    val member: String? = null,
    val text: String? = null
) : Model()

open class Model {
    @Id
    @SerializedName(value = "id", alternate = ["_id"])
    var id: String? = null

    var createdAt: Instant? = null
}

open class Edge : Model() {
    @From
    @SerializedName(value = "from", alternate = ["_from"])
    var from: String? = null

    @To
    @SerializedName(value = "to", alternate = ["_to"])
    var to: String? = null
}
