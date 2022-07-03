package com.queatz.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {

    val jwt = object {
        val secret = "ai la ai la ai la ai"
        val issuer = "http://0.0.0.0:8080/"
        val audience = "http://0.0.0.0:8080/"
        val realm = "Ai La Ai"
    }

    authentication {
        jwt {
            val jwtAudience = jwt.audience
            realm = jwt.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwt.secret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwt.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
