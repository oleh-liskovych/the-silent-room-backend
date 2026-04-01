package com.silentroom.service

import com.silentroom.data.table.Users
import com.silentroom.dto.request.CreateUserRequest
import com.silentroom.dto.request.UpdateUserRequest
import com.silentroom.dto.response.UserResponse
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class UserService {

    fun createUser(request: CreateUserRequest): UserResponse {
        return transaction {
            val existingUsername = Users.selectAll().where { Users.username eq request.username }.singleOrNull()
            if (existingUsername != null) {
                throw IllegalArgumentException("Username '${request.username}' is already taken")
            }

            val existingEmail = Users.selectAll().where { Users.email eq request.email }.singleOrNull()
            if (existingEmail != null) {
                throw IllegalArgumentException("Email '${request.email}' is already registered")
            }

            val now = Clock.System.now()
            val id = UUID.randomUUID()
            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())

            Users.insert {
                it[Users.id] = id
                it[username] = request.username
                it[displayName] = request.displayName
                it[email] = request.email
                it[Users.passwordHash] = passwordHash
                it[createdAt] = now
                it[updatedAt] = now
            }

            Users.selectAll().where { Users.id eq id }.single().toUserResponse()
        }
    }

    fun getUserById(userId: UUID): UserResponse? {
        return transaction {
            Users.selectAll().where { Users.id eq userId }.singleOrNull()?.toUserResponse()
        }
    }

    fun getUserByUsername(username: String): UserResponse? {
        return transaction {
            Users.selectAll().where { Users.username eq username }.singleOrNull()?.toUserResponse()
        }
    }

    fun getUserByEmail(email: String): ResultRow? {
        return transaction {
            Users.selectAll().where { Users.email eq email }.singleOrNull()
        }
    }

    fun getUsers(search: String? = null, page: Int = 1, pageSize: Int = 20): List<UserResponse> {
        return transaction {
            val query = Users.selectAll()
            if (!search.isNullOrBlank()) {
                query.where {
                    (Users.username like "%$search%") or (Users.displayName like "%$search%")
                }
            }
            query
                .orderBy(Users.username to SortOrder.ASC)
                .limit(pageSize)
                .offset(((page - 1) * pageSize).toLong())
                .map { it.toUserResponse() }
        }
    }

    fun updateUser(userId: UUID, request: UpdateUserRequest): UserResponse? {
        return transaction {
            val updated = Users.update({ Users.id eq userId }) {
                request.displayName?.let { name -> it[displayName] = name }
                request.email?.let { email -> it[Users.email] = email }
                request.password?.let { password ->
                    it[passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt())
                }
                it[updatedAt] = Clock.System.now()
            }
            if (updated > 0) {
                Users.selectAll().where { Users.id eq userId }.single().toUserResponse()
            } else null
        }
    }

    fun updateProfilePicture(userId: UUID, url: String?): UserResponse? {
        return transaction {
            val updated = Users.update({ Users.id eq userId }) {
                it[profilePictureUrl] = url
                it[updatedAt] = Clock.System.now()
            }
            if (updated > 0) {
                Users.selectAll().where { Users.id eq userId }.single().toUserResponse()
            } else null
        }
    }

    fun deleteProfilePicture(userId: UUID): UserResponse? {
        return updateProfilePicture(userId, null)
    }

    fun validateCredentials(email: String, password: String): ResultRow? {
        return transaction {
            val user = Users.selectAll().where { Users.email eq email }.singleOrNull() ?: return@transaction null
            if (BCrypt.checkpw(password, user[Users.passwordHash])) user else null
        }
    }

    private fun ResultRow.toUserResponse(): UserResponse {
        return UserResponse(
            id = this[Users.id].value.toString(),
            username = this[Users.username],
            displayName = this[Users.displayName],
            email = this[Users.email],
            profilePictureUrl = this[Users.profilePictureUrl],
            createdAt = this[Users.createdAt].toString()
        )
    }
}
