package com.starter.user.services

import com.starter.user.models.FieldUpdate
import com.starter.user.models.ProfileDeletedException
import com.starter.user.models.ProfileNotFoundException
import com.starter.user.models.ProfilePatch
import com.starter.user.models.ProfileResponse
import com.starter.user.models.ProfileValidationException
import com.starter.user.models.PublicProfileResponse
import com.starter.user.models.UserProfile
import com.starter.user.repository.UserProfileRepository
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Instant

private val LOCALE_REGEX = Regex("^[a-z]{2}(-[A-Z]{2})?$")
private const val DISPLAY_NAME_MAX = 80
private const val BIO_MAX = 500
private const val AVATAR_URL_MAX = 2048

class UserProfileService(
    private val repository: UserProfileRepository,
    private val clock: () -> Instant = Instant::now,
) {
    suspend fun getOwnProfile(userId: Long): ProfileResponse {
        val profile = repository.findById(userId) ?: repository.create(userId, clock())
        if (profile.deletedAt != null) throw ProfileDeletedException(userId)
        return profile.toResponse()
    }

    suspend fun updateOwnProfile(userId: Long, body: JsonObject): ProfileResponse {
        val patch = buildPatch(body) // also validates
        val existing = repository.findById(userId)
        val target = existing ?: repository.create(userId, clock())
        if (target.deletedAt != null) throw ProfileDeletedException(userId)
        val updated = repository.update(userId, patch, clock())
            ?: throw IllegalStateException("Profile vanished between create and update for userId=$userId")
        return updated.toResponse()
    }

    suspend fun deleteOwnProfile(userId: Long) {
        repository.softDelete(userId, clock())
        // Idempotent: ignore the boolean. Already-deleted or never-existed both return 204.
    }

    suspend fun getPublicProfile(targetUserId: Long): PublicProfileResponse {
        val profile = repository.findById(targetUserId)
            ?: throw ProfileNotFoundException(targetUserId)
        if (profile.deletedAt != null) throw ProfileNotFoundException(targetUserId)
        return PublicProfileResponse(
            userId = profile.userId,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            bio = profile.bio,
        )
    }

    // --- internals ---

    private fun buildPatch(body: JsonObject): ProfilePatch {
        return ProfilePatch(
            displayName = body.fieldUpdate("displayName") { v -> validateDisplayName(v) },
            bio = body.fieldUpdate("bio") { v -> validateLen("bio", v, BIO_MAX) },
            avatarUrl = body.fieldUpdate("avatarUrl") { v -> validateLen("avatarUrl", v, AVATAR_URL_MAX) },
            locale = body.fieldUpdate("locale") { v -> validateLocale(v) },
        )
    }

    // Reads a key from the JSON body. Absent key -> Unchanged. Null or "" -> SetTo(null).
    // Otherwise normalize (trim) and validate.
    private fun JsonObject.fieldUpdate(
        key: String,
        validate: (String) -> String,
    ): FieldUpdate<String?> {
        if (!containsKey(key)) return FieldUpdate.Unchanged
        val element = this[key]
        if (element is JsonNull || element == null) return FieldUpdate.SetTo(null)
        val str = (element as? JsonPrimitive)?.contentOrNull
            ?: throw ProfileValidationException("Field '$key' must be a string or null")
        if (str.isBlank()) return FieldUpdate.SetTo(null)
        return FieldUpdate.SetTo(validate(str.trim()))
    }

    private fun validateDisplayName(v: String): String {
        if (v.length > DISPLAY_NAME_MAX) {
            throw ProfileValidationException("displayName must be at most $DISPLAY_NAME_MAX characters")
        }
        // length >= 1 guaranteed by the blank-check in fieldUpdate
        return v
    }

    private fun validateLen(field: String, v: String, max: Int): String {
        if (v.length > max) throw ProfileValidationException("$field must be at most $max characters")
        return v
    }

    private fun validateLocale(v: String): String {
        if (!LOCALE_REGEX.matches(v)) {
            throw ProfileValidationException("locale must match $LOCALE_REGEX")
        }
        return v
    }

    private fun UserProfile.toResponse() = ProfileResponse(
        userId = userId,
        displayName = displayName,
        bio = bio,
        avatarUrl = avatarUrl,
        locale = locale,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}
