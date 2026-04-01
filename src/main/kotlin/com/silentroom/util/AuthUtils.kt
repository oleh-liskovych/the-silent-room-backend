package com.silentroom.util

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*

fun RoutingCall.userId(): String {
    return principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
        ?: throw IllegalStateException("User ID not found in token")
}

fun RoutingCall.username(): String {
    return principal<JWTPrincipal>()?.payload?.getClaim("username")?.asString()
        ?: throw IllegalStateException("Username not found in token")
}
