package com.queatz

import kotlinx.serialization.Serializable

@Serializable
data class Secrets(
    val jwt: SecretsJwt,
    val hms: SecretsHms,
    val gms: SecretsGms,
    val stability: SecretsStability,
)

@Serializable
data class SecretsHms(
    val appId: String,
    val clientId: String,
    val clientSecret: String
)

@Serializable
data class SecretsGms(
    val appId: String,
    val clientId: String,
    val clientEmail: String,
    val privateKeyId: String,
    val privateKey: String
)

@Serializable
data class SecretsJwt(
    val secret: String
)

@Serializable
data class SecretsStability(
    val key: String
)
