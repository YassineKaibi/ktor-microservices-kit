package com.starter.user.routes

import com.starter.user.http.RequireUserId
import com.starter.user.http.userId
import com.starter.user.models.ErrorResponse
import com.starter.user.services.UserProfileService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonObject

fun Route.userRoutes(service: UserProfileService) {
    // Public health endpoint — RequireUserId is NOT installed here.
    get("/users/health") {
        call.respond(mapOf("status" to "ok"))
    }

    // Install RequireUserId at the /me sub-route rather than at /users so that
    // the health endpoint (which shares the `users` node in Ktor's routing tree)
    // is not affected by the plugin.
    route("/users/me") {
        install(RequireUserId)

        get {
            val profile = service.getOwnProfile(call.userId)
            call.respond(profile)
        }

        put {
            val body = call.receive<JsonObject>()
            val profile = service.updateOwnProfile(call.userId, body)
            call.respond(profile)
        }

        delete {
            service.deleteOwnProfile(call.userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/users/{id}") {
        install(RequireUserId)

        get {
            val idStr = call.parameters["id"]
            val id = idStr?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                return@get
            }
            val profile = service.getPublicProfile(id)
            call.respond(profile)
        }
    }
}
