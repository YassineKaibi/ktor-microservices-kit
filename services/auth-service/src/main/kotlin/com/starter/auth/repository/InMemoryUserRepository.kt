package com.starter.auth.repository

import com.starter.auth.models.EmailAlreadyExistsException
import com.starter.auth.models.User
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * In-process backend selected via REPO_BACKEND=memory so the stack runs with no external DB.
 * State is per-process and not shared across replicas — intended for local runs and tests.
 */
class InMemoryUserRepository : UserRepository {

    private val byId = ConcurrentHashMap<Long, User>()
    private val idByEmail = ConcurrentHashMap<String, Long>()
    private val idSeq = AtomicLong(0)

    override suspend fun findByEmail(email: String): User? = idByEmail[email]?.let { byId[it] }

    override suspend fun findById(id: Long): User? = byId[id]

    override suspend fun create(email: String, passwordHash: String): User {
        val id = idSeq.incrementAndGet()
        // putIfAbsent is atomic, so concurrent registrations for the same email converge on a
        // single winner and the loser throws — mirroring the Postgres unique-index guard.
        if (idByEmail.putIfAbsent(email, id) != null) {
            throw EmailAlreadyExistsException(email)
        }
        val now = Instant.now()
        val user = User(id = id, email = email, passwordHash = passwordHash, createdAt = now, updatedAt = now)
        byId[id] = user
        return user
    }
}
