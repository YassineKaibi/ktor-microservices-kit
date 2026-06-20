package com.starter.user.repository

import com.starter.user.models.UserProfile
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process backend selected via REPO_BACKEND=memory so the stack runs with no external DB.
 * State is per-process and not shared across replicas — intended for local runs and tests.
 */
class InMemoryUserRepository : UserRepository {

    private val profiles = ConcurrentHashMap<Long, UserProfile>()

    override suspend fun findById(id: Long): UserProfile? = profiles[id]

    override suspend fun create(id: Long): UserProfile {
        // computeIfAbsent is atomic, so concurrent first requests converge on the single
        // stored profile rather than overwriting each other (mirrors the Postgres guard).
        val now = Instant.now()
        return profiles.computeIfAbsent(id) {
            UserProfile(id = id, displayName = null, bio = null, createdAt = now, updatedAt = now)
        }
    }

    override suspend fun upsert(id: Long, displayName: String?, bio: String?): UserProfile {
        val now = Instant.now()
        return profiles.compute(id) { _, existing ->
            existing?.copy(displayName = displayName, bio = bio, updatedAt = now)
                ?: UserProfile(id, displayName, bio, now, now)
        }!!
    }

    override suspend fun delete(id: Long): Boolean = profiles.remove(id) != null
}
