package com.starter.auth.repository

import com.starter.auth.models.User

interface UserRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: Long): User?
    suspend fun create(email: String, passwordHash: String): User
}
