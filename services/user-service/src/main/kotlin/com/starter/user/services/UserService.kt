package com.starter.user.services

import com.starter.user.models.UserProfile
import com.starter.user.repository.UserRepository

class UserService(
    private val userRepository: UserRepository,
) {
    /** The caller's own profile, lazily created on first access since registration lives in auth-service. */
    suspend fun getOrCreateMe(userId: Long): UserProfile =
        userRepository.findById(userId) ?: userRepository.create(userId)

    suspend fun getById(id: Long): UserProfile? = userRepository.findById(id)

    suspend fun updateMe(userId: Long, displayName: String?, bio: String?): UserProfile =
        userRepository.upsert(userId, displayName, bio)

    suspend fun deleteMe(userId: Long): Boolean = userRepository.delete(userId)
}
