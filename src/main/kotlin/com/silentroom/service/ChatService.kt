package com.silentroom.service

import com.silentroom.data.table.*
import com.silentroom.dto.response.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ChatService {

    fun getRoomsForUser(userId: UUID): List<RoomResponse> {
        return transaction {
            val roomIds = RoomMembers.selectAll()
                .where { RoomMembers.userId eq userId }
                .map { it[RoomMembers.roomId].value }

            roomIds.map { roomId ->
                val room = Rooms.selectAll().where { Rooms.id eq roomId }.single()
                val members = RoomMembers
                    .innerJoin(Users, { RoomMembers.userId }, { Users.id })
                    .selectAll()
                    .where { RoomMembers.roomId eq roomId }
                    .map {
                        RoomMemberResponse(
                            userId = it[Users.id].value.toString(),
                            username = it[Users.username],
                            displayName = it[Users.displayName],
                            profilePictureUrl = it[Users.profilePictureUrl]
                        )
                    }

                val lastMessage = Messages.selectAll()
                    .where { Messages.roomId eq roomId }
                    .orderBy(Messages.createdAt to SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()?.toMessageResponse()

                RoomResponse(
                    id = room[Rooms.id].value.toString(),
                    name = room[Rooms.name],
                    isGroup = room[Rooms.isGroup],
                    members = members,
                    lastMessage = lastMessage,
                    createdAt = room[Rooms.createdAt].toString()
                )
            }.sortedByDescending { it.lastMessage?.createdAt ?: it.createdAt }
        }
    }

    fun getMessages(roomId: UUID, userId: UUID, page: Int = 1, pageSize: Int = 50): MessagePage {
        return transaction {
            val isMember = RoomMembers.selectAll().where {
                (RoomMembers.roomId eq roomId) and (RoomMembers.userId eq userId)
            }.count() > 0

            if (!isMember) throw IllegalArgumentException("You are not a member of this room")

            val totalCount = Messages.selectAll().where { Messages.roomId eq roomId }.count()
            val totalPages = ((totalCount + pageSize - 1) / pageSize).toInt()

            val messages = Messages.selectAll()
                .where { Messages.roomId eq roomId }
                .orderBy(Messages.createdAt to SortOrder.DESC)
                .limit(pageSize)
                .offset(((page - 1) * pageSize).toLong())
                .map { it.toMessageResponse() }
                .reversed()

            MessagePage(
                messages = messages,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                totalCount = totalCount
            )
        }
    }

    fun createMessage(roomId: UUID, senderId: UUID, content: String, messageType: String = "TEXT"): MessageResponse {
        return transaction {
            val now = Clock.System.now()
            val id = UUID.randomUUID()

            Messages.insert {
                it[Messages.id] = id
                it[Messages.roomId] = roomId
                it[Messages.senderId] = senderId
                it[Messages.content] = content
                it[Messages.messageType] = messageType
                it[Messages.status] = "SENT"
                it[createdAt] = now
                it[updatedAt] = now
            }

            Rooms.update({ Rooms.id eq roomId }) {
                it[updatedAt] = now
            }

            Messages.selectAll().where { Messages.id eq id }.single().toMessageResponse()
        }
    }

    fun updateMessageStatus(messageId: UUID, status: String): MessageResponse? {
        return transaction {
            val updated = Messages.update({ Messages.id eq messageId }) {
                it[Messages.status] = status
                it[updatedAt] = Clock.System.now()
            }
            if (updated > 0) {
                Messages.selectAll().where { Messages.id eq messageId }.single().toMessageResponse()
            } else null
        }
    }

    fun markMessagesAsRead(roomId: UUID, userId: UUID): Int {
        return transaction {
            val now = Clock.System.now()
            val unreadMessages = Messages.selectAll().where {
                (Messages.roomId eq roomId) and
                (Messages.senderId neq userId) and
                (Messages.status neq "SEEN")
            }.map { it[Messages.id].value }

            var count = 0
            for (msgId in unreadMessages) {
                val exists = MessageReadReceipts.selectAll().where {
                    (MessageReadReceipts.messageId eq msgId) and (MessageReadReceipts.userId eq userId)
                }.count() > 0

                if (!exists) {
                    MessageReadReceipts.insert {
                        it[id] = UUID.randomUUID()
                        it[messageId] = msgId
                        it[MessageReadReceipts.userId] = userId
                        it[readAt] = now
                    }
                    Messages.update({ Messages.id eq msgId }) {
                        it[status] = "SEEN"
                        it[updatedAt] = now
                    }
                    count++
                }
            }
            count
        }
    }

    fun getRoomById(roomId: UUID): RoomResponse? {
        return transaction {
            val room = Rooms.selectAll().where { Rooms.id eq roomId }.singleOrNull() ?: return@transaction null
            val members = RoomMembers
                .innerJoin(Users, { RoomMembers.userId }, { Users.id })
                .selectAll()
                .where { RoomMembers.roomId eq roomId }
                .map {
                    RoomMemberResponse(
                        userId = it[Users.id].value.toString(),
                        username = it[Users.username],
                        displayName = it[Users.displayName],
                        profilePictureUrl = it[Users.profilePictureUrl]
                    )
                }

            RoomResponse(
                id = room[Rooms.id].value.toString(),
                name = room[Rooms.name],
                isGroup = room[Rooms.isGroup],
                members = members,
                createdAt = room[Rooms.createdAt].toString()
            )
        }
    }

    fun isRoomMember(roomId: UUID, userId: UUID): Boolean {
        return transaction {
            RoomMembers.selectAll().where {
                (RoomMembers.roomId eq roomId) and (RoomMembers.userId eq userId)
            }.count() > 0
        }
    }

    private fun ResultRow.toMessageResponse(): MessageResponse {
        val sender = Users.selectAll().where { Users.id eq this@toMessageResponse[Messages.senderId] }.single()
        return MessageResponse(
            id = this[Messages.id].value.toString(),
            roomId = this[Messages.roomId].value.toString(),
            senderId = this[Messages.senderId].value.toString(),
            senderUsername = sender[Users.username],
            senderDisplayName = sender[Users.displayName],
            content = this[Messages.content],
            messageType = this[Messages.messageType],
            status = this[Messages.status],
            createdAt = this[Messages.createdAt].toString()
        )
    }
}
