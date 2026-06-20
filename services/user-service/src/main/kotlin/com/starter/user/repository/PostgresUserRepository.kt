package com.starter.user.repository

import com.starter.user.models.UserProfile
import com.starter.user.models.UserProfiles
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsertReturning
import java.sql.Connection
import java.time.Instant

class PostgresUserRepository : UserRepository {

    override suspend fun findById(id: Long): UserProfile? = newSuspendedTransaction {
        UserProfiles.selectAll().where { UserProfiles.id eq id }
            .map { it.toUserProfile() }
            .singleOrNull()
    }

    override suspend fun create(id: Long): UserProfile =
        // Idempotent lazy create: INSERT ... ON CONFLICT DO NOTHING never raises on a
        // concurrent first request, so there is no aborted transaction to recover from.
        // READ COMMITTED (vs. the connection-default REPEATABLE READ) guarantees the
        // following SELECT sees the row whether we or the racing request inserted it, so
        // both callers converge on the single persisted profile and neither gets a 500.
        newSuspendedTransaction(transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            val now = Instant.now()
            UserProfiles.insertIgnore {
                it[UserProfiles.id] = id
                it[displayName] = null
                it[bio] = null
                it[createdAt] = now
                it[updatedAt] = now
            }
            UserProfiles.selectAll().where { UserProfiles.id eq id }
                .map { it.toUserProfile() }
                .single()
        }

    override suspend fun upsert(id: Long, displayName: String?, bio: String?): UserProfile =
        // Atomic provision-or-update: INSERT ... ON CONFLICT (id) DO UPDATE in a single
        // statement, so concurrent PUTs are last-write-wins with no check-then-insert TOCTOU
        // (which would 500 on a unique violation). createdAt is excluded from the update so it
        // is preserved across updates. READ COMMITTED (vs. the connection-default REPEATABLE
        // READ) avoids serialization errors on the conflict path — same reason as create().
        // RETURNING yields the final row directly, so no follow-up SELECT is needed.
        newSuspendedTransaction(transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            val now = Instant.now()
            UserProfiles.upsertReturning(onUpdateExclude = listOf(UserProfiles.createdAt)) {
                it[UserProfiles.id] = id
                it[UserProfiles.displayName] = displayName
                it[UserProfiles.bio] = bio
                it[createdAt] = now
                it[updatedAt] = now
            }.map { it.toUserProfile() }.single()
        }

    override suspend fun delete(id: Long): Boolean = newSuspendedTransaction {
        UserProfiles.deleteWhere { UserProfiles.id eq id } > 0
    }

    private fun ResultRow.toUserProfile() = UserProfile(
        id = this[UserProfiles.id],
        displayName = this[UserProfiles.displayName],
        bio = this[UserProfiles.bio],
        createdAt = this[UserProfiles.createdAt],
        updatedAt = this[UserProfiles.updatedAt],
    )
}
