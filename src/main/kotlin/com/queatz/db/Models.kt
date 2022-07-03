package com.queatz.db

import com.arangodb.entity.From
import com.arangodb.entity.Id
import com.arangodb.entity.To
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Instant

class Person : Model() {
    var name: String? = null
    var photo: String? = null
    var inviter: String? = null
}

class Settings : Model() {
    var person: String? = null
    var language: String? = null
}

class Card : Model() {
    var person: String? = null
    var name: String? = null
    var location: String? = null
    var geo: List<Double>? = null
    var conversation: String? = null
}

class Group : Model()

class Member : Model() {
    val group: String? = null
    val person: String? = null
}

class Message : Model() {
    val group: String? = null
    val person: String? = null
    val text: String? = null
}

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
