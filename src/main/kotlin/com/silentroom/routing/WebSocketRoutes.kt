package com.silentroom.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.silentroom.service.ChatService
import com.silentroom.service.WebSocketService
import com.silentroom.service.WsMessage
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.util.*

fun Route.webSocketRoutes(chatService: ChatService, webSocketService: WebSocketService) {

    webSocket("/ws/{token}") {
        val token = call.parameters["token"] ?: run {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No token provided"))
            return@webSocket
        }

        val config = call.application.environment.config
        val secret = config.property("jwt.secret").getString()
        val audience = config.property("jwt.audience").getString()
        val issuer = config.property("jwt.issuer").getString()

        val decodedJWT = try {
            JWT.require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }

        val userId = decodedJWT.getClaim("userId").asString()

        webSocketService.addConnection(userId, this)

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val wsMessage = webSocketService.parseMessage(text)

                    if (wsMessage == null) {
                        send(Frame.Text("""{"type":"ERROR","error":"Invalid message format"}"""))
                        return@consumeEach
                    }

                    when (wsMessage.type) {
                        "JOIN_ROOM" -> {
                            val roomId = wsMessage.roomId ?: return@consumeEach
                            val uid = UUID.fromString(userId)
                            if (chatService.isRoomMember(UUID.fromString(roomId), uid)) {
                                webSocketService.joinRoom(userId, roomId)
                                send(Frame.Text("""{"type":"ROOM_JOINED","roomId":"$roomId"}"""))
                            } else {
                                send(Frame.Text("""{"type":"ERROR","error":"Not a member of this room"}"""))
                            }
                        }

                        "SEND_MESSAGE" -> {
                            val roomId = wsMessage.roomId ?: return@consumeEach
                            val content = wsMessage.content ?: return@consumeEach
                            val messageType = wsMessage.messageType ?: "TEXT"

                            val message = chatService.createMessage(
                                roomId = UUID.fromString(roomId),
                                senderId = UUID.fromString(userId),
                                content = content,
                                messageType = messageType
                            )

                            val outMsg = webSocketService.toWsMessage(message)
                            webSocketService.broadcastToRoom(roomId, outMsg)
                        }

                        "IS_TYPING" -> {
                            val roomId = wsMessage.roomId ?: return@consumeEach
                            val typingMsg = WsMessage(
                                type = "IS_TYPING",
                                roomId = roomId,
                                senderId = userId,
                                senderUsername = wsMessage.senderUsername,
                                senderDisplayName = wsMessage.senderDisplayName
                            )
                            webSocketService.broadcastToRoom(roomId, typingMsg, excludeUserId = userId)
                        }

                        "MESSAGE_READ" -> {
                            val roomId = wsMessage.roomId ?: return@consumeEach
                            val uid = UUID.fromString(userId)
                            val count = chatService.markMessagesAsRead(UUID.fromString(roomId), uid)
                            if (count > 0) {
                                val readMsg = WsMessage(
                                    type = "MESSAGES_READ",
                                    roomId = roomId,
                                    senderId = userId
                                )
                                webSocketService.broadcastToRoom(roomId, readMsg, excludeUserId = userId)
                            }
                        }

                        else -> {
                            send(Frame.Text("""{"type":"ERROR","error":"Unknown message type: ${wsMessage.type}"}"""))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Connection closed
        } finally {
            webSocketService.removeConnection(userId, this)
        }
    }
}
