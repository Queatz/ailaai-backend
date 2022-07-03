package com.queatz.db


fun collections() = listOf(
    Model::class.db {},
    Person::class.db {},
    Settings::class.db {},
    Card::class.db {},
    Group::class.db {},
    Member::class.db {},
    Message::class.db {}
)
