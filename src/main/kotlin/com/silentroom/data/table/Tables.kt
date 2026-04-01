package com.silentroom.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : UUIDTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val displayName = varchar("display_name", 100)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val profilePictureUrl = varchar("profile_picture_url", 512).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object Contacts : UUIDTable("contacts") {
    val userId = reference("user_id", Users)
    val contactId = reference("contact_id", Users)
    val status = varchar("status", 20) // PENDING, ACCEPTED, REJECTED
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex("uq_user_contact", userId, contactId)
    }
}

object Rooms : UUIDTable("rooms") {
    val name = varchar("name", 100).nullable()
    val isGroup = bool("is_group").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object RoomMembers : UUIDTable("room_members") {
    val roomId = reference("room_id", Rooms)
    val userId = reference("user_id", Users)
    val joinedAt = timestamp("joined_at")

    init {
        uniqueIndex("uq_room_member", roomId, userId)
    }
}

object Messages : UUIDTable("messages") {
    val roomId = reference("room_id", Rooms)
    val senderId = reference("sender_id", Users)
    val content = text("content")
    val messageType = varchar("message_type", 20).default("TEXT") // TEXT, IMAGE, VIDEO
    val status = varchar("status", 20).default("CREATED") // CREATED, SENT, RECEIVED, SEEN
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object MessageReadReceipts : UUIDTable("message_read_receipts") {
    val messageId = reference("message_id", Messages)
    val userId = reference("user_id", Users)
    val readAt = timestamp("read_at")

    init {
        uniqueIndex("uq_message_read", messageId, userId)
    }
}

object RevokedTokens : UUIDTable("revoked_tokens") {
    val token = text("token")
    val userId = reference("user_id", Users)
    val revokedAt = timestamp("revoked_at")
}
