package com.queatz.api

data class SignUpRequest(
    val code: String
)

data class SignOnRequest(
    val email: String,
    val code: String?
)
