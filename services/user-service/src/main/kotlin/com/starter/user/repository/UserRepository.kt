package com.starter.user.repository

import com.starter.user.models.UserProfile

interface UserRepository {
    suspend fun findById(id: Long): UserProfile?

    /** Creates an empty profile for the given user id (used on first access of /users/me). */
    suspend fun create(id: Long): UserProfile

    /** Inserts or updates the profile fields for the given user id, returning the result. */
    suspend fun upsert(id: Long, displayName: String?, bio: String?): UserProfile

    suspend fun delete(id: Long): Boolean
}
