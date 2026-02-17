package com.starter.auth.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import redis.clients.jedis.JedisPool
import java.util.*

class TokenService(
    private val secret: String,
    private val issuer: String,
    private val accessExpirationMs: Long,
    private val refreshExpirationMs: Long,
    private val redisPool: JedisPool
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun generateAccessToken(userId: Long): String {
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.toString())
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + accessExpirationMs))
            .sign(algorithm)
    }

    fun generateRefreshToken(userId: Long): String {
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId.toString())
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpirationMs))
            .sign(algorithm)
    }

    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }

    fun blacklist(jti: String, expiresAt: Date) {
        val ttlSeconds = ((expiresAt.time - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
        redisPool.resource.use { jedis ->
            jedis.setex("blacklist:$jti", ttlSeconds, "1")
        }
    }

    fun isBlacklisted(jti: String): Boolean {
        redisPool.resource.use { jedis ->
            return jedis.exists("blacklist:$jti")
        }
    }
}
