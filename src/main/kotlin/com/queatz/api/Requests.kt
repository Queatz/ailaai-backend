package com.queatz.api

import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(
    val code: String?
)

@Serializable
data class SignInRequest(
    val code: String? = null,
    val link: String? = null
)
