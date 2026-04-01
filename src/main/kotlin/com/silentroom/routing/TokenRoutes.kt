package com.silentroom.routing

import com.silentroom.dto.request.LoginRequest
import com.silentroom.dto.response.ErrorResponse
import com.silentroom.dto.response.SuccessResponse
import com.silentroom.dto.response.TokenResponse
import com.silentroom.data.table.Users
import com.silentroom.service.TokenService
import com.silentroom.service.UserService
import com.silentroom.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tokenRoutes() {
    val userService by inject<UserService>()
    val tokenService by inject<TokenService>()

    route("/api/token") {
        post {
            val request = call.receive<LoginRequest>()
            val user = userService.validateCredentials(request.email, request.password)
            if (user != null) {
                val token = tokenService.generateToken(
                    userId = user[Users.id].value,
                    username = user[Users.username]
                )
                call.respond(TokenResponse(
                    accessToken = token,
                    expiresIn = 86400
                ))
            } else {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Invalid email or password"))
            }
        }

        authenticate("auth-jwt") {
            delete {
                val uid = call.userId()
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                tokenService.revokeToken(token, UUID.fromString(uid))
                call.respond(SuccessResponse("Token revoked successfully"))
            }
        }
    }
}
