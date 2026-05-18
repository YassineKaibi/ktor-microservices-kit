package com.starter.user.repository

import com.starter.user.models.ProfilePatch
import com.starter.user.models.UserProfile
import java.time.Instant

interface UserProfileRepository {
    // Returns the row even if soft-deleted; soft-delete policy lives in the service layer.
    suspend fun findById(userId: Long): UserProfile?

    // Inserts an empty profile (all profile fields null, created_at = updated_at = now).
    suspend fun create(userId: Long, now: Instant): UserProfile

    // Applies the patch to the row identified by userId, bumps updated_at,
    // returns the updated row. Returns null if no row exists.
    suspend fun update(userId: Long, patch: ProfilePatch, now: Instant): UserProfile?

    // Sets deleted_at = now if a non-deleted row exists. Returns true if a row was modified.
    suspend fun softDelete(userId: Long, now: Instant): Boolean
}
