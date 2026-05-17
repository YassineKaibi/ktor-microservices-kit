package com.starter.auth.repository

import com.starter.auth.models.User
import com.starter.auth.models.Users
import com.starter.auth.models.EmailAlreadyExistsException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.time.Instant

class PostgresUserRepository : UserRepository {

    override suspend fun findByEmail(email: String): User? = newSuspendedTransaction {
        Users.selectAll().where { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun findById(id: Long): User? = newSuspendedTransaction {
        Users.selectAll().where { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    override suspend fun create(email: String, passwordHash: String): User = newSuspendedTransaction {
        val now = Instant.now()
        try {
            val id = Users.insertAndGetId {
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
                it[Users.createdAt] = now
                it[Users.updatedAt] = now
            }.value
            User(id = id, email = email, passwordHash = passwordHash, createdAt = now, updatedAt = now)
        } catch (e: ExposedSQLException) {
            if (e.sqlState == "23505") {
                throw EmailAlreadyExistsException(email)
            }
            throw e
        }
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id].value,
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        createdAt = this[Users.createdAt],
        updatedAt = this[Users.updatedAt]
    )
}
