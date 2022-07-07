package com.queatz

data class Secrets(
    val jwt: SecretsJwt,
    val hms: SecretsHms,
    val gms: SecretsGms,
)

data class SecretsHms(
    val appId: String,
    val clientId: String,
    val clientSecret: String
)

data class SecretsGms(
    val appId: String,
    val clientId: String,
    val clientEmail: String,
    val privateKeyId: String,
    val privateKey: String
)

data class SecretsJwt(
    val secret: String
)
