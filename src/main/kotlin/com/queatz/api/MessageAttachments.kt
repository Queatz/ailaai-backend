package com.queatz.api

class CardAttachment(
    var card: String? = null
) : MessageAttachment() {
    override val type = "card"
}

class PhotosAttachment(
    var photos: List<String>? = null
) : MessageAttachment() {
    override val type = "photos"
}

abstract class MessageAttachment  {
    abstract val type: String
}
