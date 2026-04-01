package com.silentroom.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.silentroom.data.table.RevokedTokens
import com.silentroom.data.table.Users
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class TokenService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expiresIn: Long
) {

    fun generateToken(userId: UUID, username: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId.toString())
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresIn))
            .sign(Algorithm.HMAC256(secret))
    }

    fun revokeToken(token: String, userId: UUID) {
        transaction {
            RevokedTokens.insert {
                it[RevokedTokens.id] = UUID.randomUUID()
                it[RevokedTokens.token] = token
                it[RevokedTokens.userId] = userId
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    fun isTokenRevoked(token: String): Boolean {
        return transaction {
            RevokedTokens.selectAll().where { RevokedTokens.token eq token }.count() > 0
        }
    }
}
