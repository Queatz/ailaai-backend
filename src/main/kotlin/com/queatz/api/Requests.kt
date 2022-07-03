package com.queatz.api

data class SignUp(
    val code: String
)

data class SignOn(
    val email: String,
    val password: String
)

data class SignOff(
    val token: String
)
