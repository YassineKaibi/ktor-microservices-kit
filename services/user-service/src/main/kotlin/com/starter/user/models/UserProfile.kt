package com.starter.user.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

// --- Exposed table ---
//
// `LongIdTable` gives us an auto-increment `id` column by default. We don't
// want that — `user_id` is supplied externally (= auth-service user id) and
// is the primary key. So we declare our own `userId` column and use it as
// the PK; the inherited `id` is unused.
object UserProfiles : LongIdTable("user_profiles", columnName = "user_id") {
    val displayName = varchar("display_name", 80).nullable()
    val bio = varchar("bio", 500).nullable()
    val avatarUrl = varchar("avatar_url", 2048).nullable()
    val locale = varchar("locale", 10).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
}

// --- Domain row ---
data class UserProfile(
    val userId: Long,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val locale: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)

// --- Internal patch type ---
//
// Distinguishes "field absent in request" (Unchanged) from "field present
// and explicitly cleared" (SetTo(null)) so the repository can emit a partial
// UPDATE that only touches columns the client sent.
sealed interface FieldUpdate<out T> {
    data object Unchanged : FieldUpdate<Nothing>
    data class SetTo<T>(val value: T) : FieldUpdate<T>
}

data class ProfilePatch(
    val displayName: FieldUpdate<String?> = FieldUpdate.Unchanged,
    val bio: FieldUpdate<String?> = FieldUpdate.Unchanged,
    val avatarUrl: FieldUpdate<String?> = FieldUpdate.Unchanged,
    val locale: FieldUpdate<String?> = FieldUpdate.Unchanged,
)

// --- DTOs (JSON wire format) ---

@Serializable
data class ProfileResponse(
    val userId: Long,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val locale: String?,
    val createdAt: String,  // ISO-8601
    val updatedAt: String,
)

@Serializable
data class PublicProfileResponse(
    val userId: Long,
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
)

// `UpdateProfileRequest` uses nullable fields with default = null so that
// kotlinx.serialization treats absent JSON keys as `null`. To preserve the
// "absent vs explicit null" distinction we route through `JsonObject` in the
// route handler (see Task 8). The data class is kept for documentation and
// for the simple-client case.
@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val locale: String? = null,
)

@Serializable
data class ErrorResponse(val error: String)

// --- Exceptions ---

class ProfileNotFoundException(userId: Long) : RuntimeException("Profile not found: $userId")
class ProfileDeletedException(userId: Long) : RuntimeException("Profile deleted: $userId")
class ProfileValidationException(message: String) : RuntimeException(message)
