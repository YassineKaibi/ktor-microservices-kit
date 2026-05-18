package com.starter.user.repository

import com.starter.user.models.FieldUpdate
import com.starter.user.models.ProfilePatch
import com.starter.user.models.UserProfile
import com.starter.user.models.UserProfiles
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class PostgresUserProfileRepository : UserProfileRepository {

    override suspend fun findById(userId: Long): UserProfile? = newSuspendedTransaction {
        UserProfiles.selectAll().where { UserProfiles.id eq userId }
            .map { it.toProfile() }
            .singleOrNull()
    }

    override suspend fun create(userId: Long, now: Instant): UserProfile = newSuspendedTransaction {
        UserProfiles.insert {
            it[UserProfiles.id] = EntityID(userId, UserProfiles)
            it[UserProfiles.createdAt] = now
            it[UserProfiles.updatedAt] = now
        }
        UserProfile(
            userId = userId,
            displayName = null,
            bio = null,
            avatarUrl = null,
            locale = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
    }

    override suspend fun update(userId: Long, patch: ProfilePatch, now: Instant): UserProfile? =
        newSuspendedTransaction {
            val rows = UserProfiles.update({ UserProfiles.id eq userId }) {
                when (val u = patch.displayName) { is FieldUpdate.SetTo -> it[UserProfiles.displayName] = u.value; FieldUpdate.Unchanged -> {} }
                when (val u = patch.bio) { is FieldUpdate.SetTo -> it[UserProfiles.bio] = u.value; FieldUpdate.Unchanged -> {} }
                when (val u = patch.avatarUrl) { is FieldUpdate.SetTo -> it[UserProfiles.avatarUrl] = u.value; FieldUpdate.Unchanged -> {} }
                when (val u = patch.locale) { is FieldUpdate.SetTo -> it[UserProfiles.locale] = u.value; FieldUpdate.Unchanged -> {} }
                it[UserProfiles.updatedAt] = now
            }
            if (rows == 0) return@newSuspendedTransaction null
            UserProfiles.selectAll().where { UserProfiles.id eq userId }
                .map { it.toProfile() }
                .single()
        }

    override suspend fun softDelete(userId: Long, now: Instant): Boolean = newSuspendedTransaction {
        val rows = UserProfiles.update({
            (UserProfiles.id eq userId) and (UserProfiles.deletedAt.isNull())
        }) {
            it[UserProfiles.deletedAt] = now
            it[UserProfiles.updatedAt] = now
        }
        rows > 0
    }

    private fun ResultRow.toProfile() = UserProfile(
        userId = this[UserProfiles.id].value,
        displayName = this[UserProfiles.displayName],
        bio = this[UserProfiles.bio],
        avatarUrl = this[UserProfiles.avatarUrl],
        locale = this[UserProfiles.locale],
        createdAt = this[UserProfiles.createdAt],
        updatedAt = this[UserProfiles.updatedAt],
        deletedAt = this[UserProfiles.deletedAt],
    )
}

// Imported locally to keep the `and` operator clear in `softDelete`.
private infix fun org.jetbrains.exposed.sql.Op<Boolean>.and(other: org.jetbrains.exposed.sql.Op<Boolean>) =
    org.jetbrains.exposed.sql.AndOp(listOf(this, other))
