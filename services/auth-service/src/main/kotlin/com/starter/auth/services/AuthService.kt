package com.starter.auth.services

import com.starter.auth.models.TokenResponse
import com.starter.auth.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class AuthService(
    private val userRepository: UserRepository,
    private val tokenService: TokenService
) {
    suspend fun register(email: String, password: String): Result<TokenResponse> {
        if (userRepository.existsByEmail(email)) {
            return Result.failure(IllegalArgumentException("Email already registered"))
        }

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12))
        val user = userRepository.create(email, passwordHash)

        return Result.success(
            TokenResponse(
                accessToken = tokenService.generateAccessToken(user.id),
                refreshToken = tokenService.generateRefreshToken(user.id)
            )
        )
    }

    suspend fun login(email: String, password: String): Result<TokenResponse> {
        val user = userRepository.findByEmail(email)
            ?: return Result.failure(IllegalArgumentException("Invalid credentials"))

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid credentials"))
        }

        return Result.success(
            TokenResponse(
                accessToken = tokenService.generateAccessToken(user.id),
                refreshToken = tokenService.generateRefreshToken(user.id)
            )
        )
    }

    fun refresh(refreshToken: String): Result<TokenResponse> {
        val decoded = tokenService.verifyToken(refreshToken)
            ?: return Result.failure(IllegalArgumentException("Invalid refresh token"))

        if (decoded.getClaim("type").asString() != "refresh") {
            return Result.failure(IllegalArgumentException("Invalid refresh token"))
        }

        if (tokenService.isBlacklisted(decoded.id)) {
            return Result.failure(IllegalArgumentException("Token has been revoked"))
        }

        // Blacklist the old refresh token
        tokenService.blacklist(decoded.id, decoded.expiresAt)

        val userId = decoded.subject.toLong()
        return Result.success(
            TokenResponse(
                accessToken = tokenService.generateAccessToken(userId),
                refreshToken = tokenService.generateRefreshToken(userId)
            )
        )
    }

    fun validateToken(token: String): Result<Long> {
        val decoded = tokenService.verifyToken(token)
            ?: return Result.failure(IllegalArgumentException("Invalid token"))

        if (decoded.getClaim("type").asString() != "access") {
            return Result.failure(IllegalArgumentException("Invalid token type"))
        }

        if (tokenService.isBlacklisted(decoded.id)) {
            return Result.failure(IllegalArgumentException("Token has been revoked"))
        }

        return Result.success(decoded.subject.toLong())
    }

    fun logout(token: String) {
        val decoded = tokenService.verifyToken(token) ?: return
        tokenService.blacklist(decoded.id, decoded.expiresAt)
    }
}
