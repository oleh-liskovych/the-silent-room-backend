package com.silentroom.routing

import com.silentroom.service.ChatService
import com.silentroom.util.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.chatRoutes(chatService: ChatService) {

    authenticate("auth-jwt") {
        route("/api/rooms") {
            get {
                val uid = UUID.fromString(call.userId())
                val rooms = chatService.getRoomsForUser(uid)
                call.respond(rooms)
            }

            get("/{id}") {
                val uid = UUID.fromString(call.userId())
                val roomId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Room ID required"))

                if (!chatService.isRoomMember(roomId, uid)) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member of this room"))
                }

                val room = chatService.getRoomById(roomId)
                if (room != null) {
                    call.respond(room)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Room not found"))
                }
            }

            get("/direct/{userId}") {
                val uid = UUID.fromString(call.userId())
                val otherUserId = call.parameters["userId"]?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID required"))

                val room = chatService.getDirectRoomBetweenUsers(uid, otherUserId)
                if (room != null) {
                    call.respond(room)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "No direct room found with this user"))
                }
            }

            get("/{id}/messages") {
                val uid = UUID.fromString(call.userId())
                val roomId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Room ID required"))
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                val messages = chatService.getMessages(roomId, uid, page, pageSize)
                call.respond(messages)
            }
        }
    }
}
