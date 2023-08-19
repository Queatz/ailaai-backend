package com.queatz.api

data class SignUpRequest(
    val code: String?
)

data class SignInRequest(
    val code: String? = null,
    val link: String? = null
)
