package com.silentroom.service

import com.silentroom.data.table.Contacts
import com.silentroom.data.table.Rooms
import com.silentroom.data.table.RoomMembers
import com.silentroom.data.table.Users
import com.silentroom.dto.response.ContactResponse
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ContactService {

    fun sendInvitation(fromUserId: UUID, toUsername: String): ContactResponse {
        return transaction {
            val toUser = Users.selectAll().where { Users.username eq toUsername }.singleOrNull()
                ?: throw IllegalArgumentException("User '$toUsername' not found")

            val toUserId = toUser[Users.id].value

            if (fromUserId == toUserId) {
                throw IllegalArgumentException("You cannot invite yourself")
            }

            val existing = Contacts.selectAll().where {
                ((Contacts.userId eq fromUserId) and (Contacts.contactId eq toUserId)) or
                ((Contacts.userId eq toUserId) and (Contacts.contactId eq fromUserId))
            }.singleOrNull()

            if (existing != null) {
                throw IllegalArgumentException("A contact relationship already exists with this user")
            }

            val now = Clock.System.now()
            val id = UUID.randomUUID()

            Contacts.insert {
                it[Contacts.id] = id
                it[userId] = fromUserId
                it[contactId] = toUserId
                it[status] = "PENDING"
                it[createdAt] = now
                it[updatedAt] = now
            }

            getContactById(id)!!
        }
    }

    fun respondToInvitation(invitationId: UUID, userId: UUID, accept: Boolean): ContactResponse {
        return transaction {
            val invitation = Contacts.selectAll().where {
                (Contacts.id eq invitationId) and (Contacts.contactId eq userId) and (Contacts.status eq "PENDING")
            }.singleOrNull() ?: throw IllegalArgumentException("Invitation not found or already responded to")

            val newStatus = if (accept) "ACCEPTED" else "REJECTED"
            Contacts.update({ Contacts.id eq invitationId }) {
                it[status] = newStatus
                it[updatedAt] = Clock.System.now()
            }

            if (accept) {
                createDirectRoom(invitation[Contacts.userId].value, invitation[Contacts.contactId].value)
            }

            getContactById(invitationId)!!
        }
    }

    fun getContacts(userId: UUID): List<ContactResponse> {
        return transaction {
            Contacts.selectAll().where {
                ((Contacts.userId eq userId) or (Contacts.contactId eq userId)) and (Contacts.status eq "ACCEPTED")
            }.map { row -> toContactResponse(row, userId) }
        }
    }

    fun getIncomingInvitations(userId: UUID): List<ContactResponse> {
        return transaction {
            Contacts.selectAll().where {
                (Contacts.contactId eq userId) and (Contacts.status eq "PENDING")
            }.map { row -> toContactResponse(row, userId) }
        }
    }

    fun getOutgoingInvitations(userId: UUID): List<ContactResponse> {
        return transaction {
            Contacts.selectAll().where {
                (Contacts.userId eq userId) and (Contacts.status eq "PENDING")
            }.map { row -> toContactResponse(row, userId) }
        }
    }

    fun removeContact(contactRecordId: UUID, userId: UUID): Boolean {
        return transaction {
            val deleted = Contacts.deleteWhere {
                (Contacts.id eq contactRecordId) and
                ((Contacts.userId eq userId) or (Contacts.contactId eq userId))
            }
            deleted > 0
        }
    }

    private fun getContactById(id: UUID): ContactResponse? {
        return transaction {
            val row = Contacts.selectAll().where { Contacts.id eq id }.singleOrNull() ?: return@transaction null
            val otherUserId = row[Contacts.contactId].value
            toContactResponse(row, row[Contacts.userId].value, otherUserId)
        }
    }

    private fun toContactResponse(row: ResultRow, currentUserId: UUID, overrideOtherUserId: UUID? = null): ContactResponse {
        val otherUserId = overrideOtherUserId ?: if (row[Contacts.userId].value == currentUserId) {
            row[Contacts.contactId].value
        } else {
            row[Contacts.userId].value
        }

        val otherUser = Users.selectAll().where { Users.id eq otherUserId }.single()

        return ContactResponse(
            id = row[Contacts.id].value.toString(),
            userId = row[Contacts.userId].value.toString(),
            contactId = row[Contacts.contactId].value.toString(),
            contactUsername = otherUser[Users.username],
            contactDisplayName = otherUser[Users.displayName],
            contactProfilePictureUrl = otherUser[Users.profilePictureUrl],
            status = row[Contacts.status],
            createdAt = row[Contacts.createdAt].toString()
        )
    }

    private fun createDirectRoom(userId1: UUID, userId2: UUID) {
        val now = Clock.System.now()
        val roomId = UUID.randomUUID()
        Rooms.insert {
            it[id] = roomId
            it[name] = null
            it[isGroup] = false
            it[createdAt] = now
            it[updatedAt] = now
        }
        RoomMembers.insert {
            it[id] = UUID.randomUUID()
            it[this.roomId] = roomId
            it[this.userId] = userId1
            it[joinedAt] = now
        }
        RoomMembers.insert {
            it[id] = UUID.randomUUID()
            it[this.roomId] = roomId
            it[this.userId] = userId2
            it[joinedAt] = now
        }
    }
}
