package com.starter.auth.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun blacklist(jti: String, expiresAt: Date): Boolean {
        val ttlSeconds = ((expiresAt.time - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
        return withContext(Dispatchers.IO) {
            redisPool.resource.use { jedis ->
                val result = jedis.set("blacklist:$jti", "1", SetParams.setParams().nx().ex(ttlSeconds))
                result == "OK"
            }
        }
    }

    suspend fun isBlacklisted(jti: String): Boolean {
        return withContext(Dispatchers.IO) {
            redisPool.resource.use { jedis ->
                return@use jedis.exists("blacklist:$jti")
            }
        }
    }
}
