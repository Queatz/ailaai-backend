package com.queatz

data class Secrets(
    val jwt: SecretsJwt
)

data class SecretsJwt(
    val secret: String
)
