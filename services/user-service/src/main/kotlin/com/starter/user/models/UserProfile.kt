package com.starter.user.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Profile data owned by user-service. The primary key is the auth user id supplied by the
 * gateway via the X-User-Id header, so it is set explicitly rather than auto-generated.
 */
object UserProfiles : Table("user_profiles") {
    val id = long("id")
    val displayName = varchar("display_name", 255).nullable()
    val bio = text("bio").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

data class UserProfile(
    val id: Long,
    val displayName: String?,
    val bio: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class UserProfileResponse(
    val id: Long,
    val displayName: String?,
    val bio: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

fun UserProfile.toResponse() = UserProfileResponse(
    id = id,
    displayName = displayName,
    bio = bio,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
