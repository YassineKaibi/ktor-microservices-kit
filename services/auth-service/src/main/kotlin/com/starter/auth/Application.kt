package com.starter.auth

import com.starter.auth.config.configureAuth
import com.starter.auth.config.configureDatabaseConfig
import com.starter.auth.config.createRedisPool
import com.starter.auth.models.ErrorResponse
import com.starter.auth.repository.PostgresUserRepository
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

    configureDatabaseConfig()

    val redisPool = createRedisPool()
    val jwtConfig = environment.config.config("jwt")
    val tokenService = TokenService(
        secret = jwtConfig.property("secret").getString(),
        issuer = jwtConfig.property("issuer").getString(),
        accessExpirationMs = jwtConfig.property("accessExpirationMs").getString().toLong(),
        refreshExpirationMs = jwtConfig.property("refreshExpirationMs").getString().toLong(),
        redisPool = redisPool
    )

    val userRepository = PostgresUserRepository()
    val authService = AuthService(
        userRepository,
        tokenService,
        bcryptCost = environment.config.property("auth.bcryptCost").getString().toInt(),
        accessExpirationMs = jwtConfig.property("accessExpirationMs").getString().toLong()
    )

    configureAuth(tokenService)

    routing {
        authRoutes(authService)
    }
}
