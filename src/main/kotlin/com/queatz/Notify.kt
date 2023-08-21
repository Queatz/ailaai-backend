package com.queatz

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.push
import java.util.logging.Logger

class Notify {
    fun message(group: Group, from: Person, message: Message) {
        val pushData = PushData(
            PushAction.Message,
            MessagePushData(
                Group().apply { id = group.id },
                Person(name = from.name).apply { id = from.id },
                message
            )
        )

        Logger.getAnonymousLogger().info("Sending message push")

        db.memberDevices(group.id!!).filter {
            it.member?.from != from.id
        }.apply {
            Logger.getAnonymousLogger().info("count: $size")
            // Un-hide any groups
            filter { it.member?.hide == true }.forEach {
                it.member!!.hide = false
                db.update(it.member!!)
            }

            // Send push
            forEach {
                it.devices?.forEach { device ->
                    push.sendPush(device, pushData)
                }
            }
        }
    }
}
