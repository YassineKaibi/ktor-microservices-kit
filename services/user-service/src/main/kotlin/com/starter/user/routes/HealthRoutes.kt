package com.starter.user.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Liveness (`/users/health`) reports the process is up. Readiness (`/users/ready`) reports the
 * datastore is reachable, so Kubernetes only routes traffic once dependencies are usable.
 * Both paths are public — the gateway forwards them without an auth_request.
 */
fun Route.healthRoutes(isReady: suspend () -> Boolean) {
    get("/users/health") {
        call.respond(mapOf("status" to "ok"))
    }

    get("/users/ready") {
        if (isReady()) {
            call.respond(mapOf("status" to "ready"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not ready"))
        }
    }
}
