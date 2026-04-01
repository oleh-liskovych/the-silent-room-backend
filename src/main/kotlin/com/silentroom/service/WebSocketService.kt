package com.silentroom.service

import com.silentroom.dto.response.MessageResponse
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class WsMessage(
    val type: String, // SEND_MESSAGE, IS_TYPING, MESSAGE_READ, JOIN_ROOM, MESSAGE_STATUS, ERROR
    val roomId: String? = null,
    val content: String? = null,
    val messageId: String? = null,
    val senderId: String? = null,
    val senderUsername: String? = null,
    val senderDisplayName: String? = null,
    val messageType: String? = null,
    val status: String? = null,
    val timestamp: String? = null,
    val error: String? = null
)

class WebSocketService {
    // userId -> set of WebSocket sessions
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()
    // roomId -> set of userIds
    private val roomMembers = ConcurrentHashMap<String, MutableSet<String>>()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun addConnection(userId: String, session: DefaultWebSocketSession) {
        connections.getOrPut(userId) { Collections.synchronizedSet(mutableSetOf()) }.add(session)
    }

    fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        connections[userId]?.remove(session)
        if (connections[userId]?.isEmpty() == true) {
            connections.remove(userId)
        }
        // Remove from all rooms
        roomMembers.forEach { (_, members) ->
            members.remove(userId)
        }
    }

    fun joinRoom(userId: String, roomId: String) {
        roomMembers.getOrPut(roomId) { Collections.synchronizedSet(mutableSetOf()) }.add(userId)
    }

    suspend fun broadcastToRoom(roomId: String, message: WsMessage, excludeUserId: String? = null) {
        val members = roomMembers[roomId] ?: return
        val messageText = json.encodeToString(message)
        members.filter { it != excludeUserId }.forEach { userId ->
            connections[userId]?.forEach { session ->
                try {
                    session.send(Frame.Text(messageText))
                } catch (e: Exception) {
                    // Session might be closed
                }
            }
        }
    }

    suspend fun sendToUser(userId: String, message: WsMessage) {
        val messageText = json.encodeToString(message)
        connections[userId]?.forEach { session ->
            try {
                session.send(Frame.Text(messageText))
            } catch (e: Exception) {
                // Session might be closed
            }
        }
    }

    fun parseMessage(text: String): WsMessage? {
        return try {
            json.decodeFromString<WsMessage>(text)
        } catch (e: Exception) {
            null
        }
    }

    fun isUserOnline(userId: String): Boolean {
        return connections.containsKey(userId) && connections[userId]?.isNotEmpty() == true
    }

    fun toWsMessage(msg: MessageResponse): WsMessage {
        return WsMessage(
            type = "NEW_MESSAGE",
            roomId = msg.roomId,
            content = msg.content,
            messageId = msg.id,
            senderId = msg.senderId,
            senderUsername = msg.senderUsername,
            senderDisplayName = msg.senderDisplayName,
            messageType = msg.messageType,
            status = msg.status,
            timestamp = msg.createdAt
        )
    }
}
