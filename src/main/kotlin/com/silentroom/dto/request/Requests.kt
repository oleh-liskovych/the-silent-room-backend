package com.silentroom.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val username: String,
    val displayName: String,
    val email: String,
    val password: String
)

@Serializable
data class UpdateUserRequest(
    val displayName: String? = null,
    val email: String? = null,
    val password: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SendInvitationRequest(
    val username: String
)

@Serializable
data class RespondInvitationRequest(
    val accept: Boolean
)
