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

        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(request.email)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid email format"))
            return@post
        }

        if (request.password.length < 8 || request.password.length > 72) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Password must be 8–72 characters"))
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

    authenticate("auth-jwt") {
        get("/auth/validate") {
            val principal = call.principal<JWTPrincipal>()!!
            val tokenType = principal.payload.getClaim("type").asString()
            if (tokenType != "access") {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            val userId = principal.payload.subject
            call.response.header("X-User-Id", userId)
            call.respond(HttpStatusCode.OK)
        }

        post("/auth/logout") {
            val authHeader = call.request.header("Authorization")!!
            val token = authHeader.removePrefix("Bearer ")
            authService.logout(token)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
        }
    }
}
