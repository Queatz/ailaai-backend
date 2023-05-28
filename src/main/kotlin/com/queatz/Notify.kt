package com.queatz

import com.queatz.db.Group
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.db.memberDevices
import com.queatz.plugins.db
import com.queatz.plugins.push

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

        db.memberDevices(group.id!!).filter {
            it.member?.from != from.id
        }.apply {
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
