package com.starter.auth

import com.starter.auth.config.configureAuth
import com.starter.auth.config.configureDatabaseConfig
import com.starter.auth.config.createRedisPool
import com.starter.auth.models.ErrorResponse
import com.starter.auth.repository.InMemoryUserRepository
import com.starter.auth.repository.PostgresUserRepository
import com.starter.auth.repository.UserRepository
import com.starter.auth.routes.authRoutes
import com.starter.auth.services.AuthService
import com.starter.auth.services.TokenService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }

    val backend = environment.config.property("repository.backend").getString()
    val userRepository: UserRepository
    val readinessCheck: suspend () -> Boolean
    when (backend) {
        "postgres" -> {
            configureDatabaseConfig()
            userRepository = PostgresUserRepository()
            readinessCheck = {
                try {
                    newSuspendedTransaction { exec("SELECT 1") }
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        "memory" -> {
            log.info("Using in-memory repository backend (no external datastore)")
            userRepository = InMemoryUserRepository()
            readinessCheck = { true }
        }
        else -> error("Unsupported repository.backend '$backend'; expected 'postgres' or 'memory'")
    }

    val redisPool = createRedisPool()
    val jwtConfig = environment.config.config("jwt")
    val tokenService = TokenService(
        secret = jwtConfig.property("secret").getString(),
        issuer = jwtConfig.property("issuer").getString(),
        accessExpirationMs = jwtConfig.property("accessExpirationMs").getString().toLong(),
        refreshExpirationMs = jwtConfig.property("refreshExpirationMs").getString().toLong(),
        redisPool = redisPool
    )

    val authService = AuthService(
        userRepository,
        tokenService,
        bcryptCost = environment.config.property("auth.bcryptCost").getString().toInt(),
        accessExpirationMs = jwtConfig.property("accessExpirationMs").getString().toLong()
    )

    configureAuth(tokenService)

    routing {
        authRoutes(authService, readinessCheck)
    }
}
