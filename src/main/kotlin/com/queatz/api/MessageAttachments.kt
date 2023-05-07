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

class AudioAttachment(
    var audio: String? = null
) : MessageAttachment() {
    override val type = "audio"
}

class VideoAttachment(
    var video: String? = null
) : MessageAttachment() {
    override val type = "video"
}

abstract class MessageAttachment  {
    abstract val type: String
}
