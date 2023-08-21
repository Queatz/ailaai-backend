package com.queatz.api

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse (
    val token: String
)
