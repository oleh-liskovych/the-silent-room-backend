package com.silentroom.routing

import com.silentroom.dto.request.CreateUserRequest
import com.silentroom.dto.request.UpdateUserRequest
import com.silentroom.dto.response.SuccessResponse
import com.silentroom.service.UserService
import com.silentroom.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.userRoutes(userService: UserService) {

    route("/api/users") {
        post {
            val request = call.receive<CreateUserRequest>()
            val user = userService.createUser(request)
            call.respond(HttpStatusCode.Created, user)
        }

        authenticate("auth-jwt") {
            get {
                val search = call.request.queryParameters["search"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val users = userService.getUsers(search, page, pageSize)
                call.respond(users)
            }

            get("/me") {
                val uid = call.userId()
                val user = userService.getUserById(UUID.fromString(uid))
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }

            get("/username/{username}") {
                val username = call.parameters["username"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Username required"))
                val user = userService.getUserByUsername(username)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }

            put("/me") {
                val uid = call.userId()
                val request = call.receive<UpdateUserRequest>()
                val user = userService.updateUser(UUID.fromString(uid), request)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }

            delete("/me/profile-picture") {
                val uid = call.userId()
                val user = userService.deleteProfilePicture(UUID.fromString(uid))
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }
        }
    }
}
