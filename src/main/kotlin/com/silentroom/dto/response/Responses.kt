package com.silentroom.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val createdAt: String
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)

@Serializable
data class ContactResponse(
    val id: String,
    val userId: String,
    val contactId: String,
    val contactUsername: String,
    val contactDisplayName: String,
    val contactProfilePictureUrl: String? = null,
    val status: String,
    val createdAt: String
)

@Serializable
data class RoomResponse(
    val id: String,
    val name: String?,
    val isGroup: Boolean,
    val members: List<RoomMemberResponse>,
    val lastMessage: MessageResponse? = null,
    val createdAt: String
)

@Serializable
data class RoomMemberResponse(
    val userId: String,
    val username: String,
    val displayName: String,
    val profilePictureUrl: String? = null
)

@Serializable
data class MessageResponse(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderUsername: String,
    val senderDisplayName: String,
    val content: String,
    val messageType: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class MessagePage(
    val messages: List<MessageResponse>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalCount: Long
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

@Serializable
data class SuccessResponse(
    val message: String
)
