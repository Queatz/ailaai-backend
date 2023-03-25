package com.queatz.api

class CardAttachment(
    var card: String? = null
) : MessageAttachment() {
    override val type = "card"
}

abstract class MessageAttachment  {
    abstract val type: String
}
