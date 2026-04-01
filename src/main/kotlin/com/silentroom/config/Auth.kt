package com.silentroom.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.silentroom.service.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject

fun Application.configureAuth() {
    val config = environment.config
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val audience = config.property("jwt.audience").getString()
    val realm = config.property("jwt.realm").getString()

    val tokenService = TokenService(
        secret = secret,
        issuer = issuer,
        audience = audience,
        expiresIn = config.property("jwt.expiresIn").getString().toLong()
    )

    // Register TokenService in Koin manually since it needs config
    val koin = org.koin.core.context.GlobalContext.get()
    koin.loadModules(listOf(org.koin.dsl.module { single { tokenService } }))

    install(Authentication) {
        jwt("auth-jwt") {
            this.realm = realm
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                val token = request.headers["Authorization"]?.removePrefix("Bearer ")
                if (token != null && tokenService.isTokenRevoked(token)) {
                    return@validate null
                }
                if (credential.payload.audience.contains(audience)) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }
}
