package com.starter.user.routes

import com.starter.user.models.ErrorResponse
import com.starter.user.models.UpdateProfileRequest
import com.starter.user.models.toResponse
import com.starter.user.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Resolves the authenticated user from the X-User-Id header injected by the gateway after its
 * auth_request to auth-service. These services never parse the JWT themselves.
 */
private suspend fun ApplicationCall.requireUserId(): Long? {
    val userId = request.header("X-User-Id")?.toLongOrNull()
    if (userId == null) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid X-User-Id"))
    }
    return userId
}

fun Route.userRoutes(userService: UserService) {
    get("/users/me") {
        val userId = call.requireUserId() ?: return@get
        val profile = userService.getOrCreateMe(userId)
        call.respond(profile.toResponse())
    }

    put("/users/me") {
        val userId = call.requireUserId() ?: return@put
        val request = call.receive<UpdateProfileRequest>()
        val profile = userService.updateMe(userId, request.displayName, request.bio)
        call.respond(profile.toResponse())
    }

    delete("/users/me") {
        val userId = call.requireUserId() ?: return@delete
        userService.deleteMe(userId)
        call.respond(HttpStatusCode.NoContent)
    }

    get("/users/{id}") {
        if (call.requireUserId() == null) return@get
        val id = call.parameters["id"]?.toLongOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
            return@get
        }
        val profile = userService.getById(id)
        if (profile == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
        } else {
            call.respond(profile.toResponse())
        }
    }
}
