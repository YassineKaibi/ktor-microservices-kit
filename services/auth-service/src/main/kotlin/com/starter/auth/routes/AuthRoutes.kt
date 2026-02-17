package com.starter.auth.routes

import com.starter.auth.models.*
import com.starter.auth.services.AuthService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    get("/auth/health") {
        call.respond(mapOf("status" to "ok"))
    }

    post("/auth/register") {
        val request = call.receive<RegisterRequest>()

        if (request.email.isBlank() || request.password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email and password are required"))
            return@post
        }

        authService.register(request.email, request.password)
            .onSuccess { tokens ->
                call.respond(HttpStatusCode.Created, tokens)
            }
            .onFailure { error ->
                call.respond(HttpStatusCode.Conflict, ErrorResponse(error.message ?: "Registration failed"))
            }
    }

    post("/auth/login") {
        val request = call.receive<LoginRequest>()

        authService.login(request.email, request.password)
            .onSuccess { tokens ->
                call.respond(tokens)
            }
            .onFailure {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            }
    }

    post("/auth/refresh") {
        val request = call.receive<RefreshRequest>()

        authService.refresh(request.refreshToken)
            .onSuccess { tokens ->
                call.respond(tokens)
            }
            .onFailure { error ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error.message ?: "Invalid refresh token"))
            }
    }

    get("/auth/validate") {
        val authHeader = call.request.header("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.respond(HttpStatusCode.Unauthorized)
            return@get
        }

        val token = authHeader.removePrefix("Bearer ")
        authService.validateToken(token)
            .onSuccess { userId ->
                call.response.header("X-User-Id", userId.toString())
                call.respond(HttpStatusCode.OK)
            }
            .onFailure {
                call.respond(HttpStatusCode.Unauthorized)
            }
    }

    authenticate("auth-jwt") {
        post("/auth/logout") {
            val authHeader = call.request.header("Authorization")!!
            val token = authHeader.removePrefix("Bearer ")
            authService.logout(token)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
        }
    }
}
