package com.starter.auth.services

import com.starter.auth.models.EmailAlreadyExistsException
import com.starter.auth.models.TokenResponse
import com.starter.auth.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory

class AuthService(
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
    private val bcryptCost: Int,
    private val accessExpirationMs: Long
) {
    private val log = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun register(email: String, password: String): Result<TokenResponse> {
        return try {
            val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(bcryptCost))
            val user = userRepository.create(email, passwordHash)
            Result.success(tokensFor(user.id))
        } catch (e: EmailAlreadyExistsException) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<TokenResponse> {
        val user = userRepository.findByEmail(email)
            ?: return Result.failure(IllegalArgumentException("Invalid credentials"))

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid credentials"))
        }

        return Result.success(tokensFor(user.id))
    }

    suspend fun refresh(refreshToken: String): Result<TokenResponse> {
        val decoded = tokenService.verifyToken(refreshToken)
            ?: return Result.failure(IllegalArgumentException("Invalid refresh token"))

        if (decoded.getClaim("type").asString() != "refresh") {
            return Result.failure(IllegalArgumentException("Invalid refresh token"))
        }

        // Atomic claim: if blacklist() returns false, another request already consumed this token.
        if (!tokenService.blacklist(decoded.id, decoded.expiresAt)) {
            return Result.failure(IllegalArgumentException("Token has been revoked"))
        }

        val userId = decoded.subject.toLong()
        return Result.success(tokensFor(userId))
    }

    suspend fun validateToken(token: String): Result<Long> {
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

    suspend fun logout(token: String) {
        val decoded = tokenService.verifyToken(token)
        if (decoded == null) {
            log.warn("logout called with invalid or expired token")
            return
        }
        tokenService.blacklist(decoded.id, decoded.expiresAt)
    }

    private fun tokensFor(userId: Long): TokenResponse = TokenResponse(
        accessToken = tokenService.generateAccessToken(userId),
        refreshToken = tokenService.generateRefreshToken(userId),
        expiresIn = accessExpirationMs / 1000
    )
}
