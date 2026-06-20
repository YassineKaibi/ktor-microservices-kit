package com.starter.crud.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Liveness (`/items/health`) reports the process is up. Readiness (`/items/ready`) reports the
 * datastore is reachable, so Kubernetes only routes traffic once dependencies are usable.
 * Both paths are public — the gateway forwards them without an auth_request.
 */
fun Route.healthRoutes(isReady: suspend () -> Boolean) {
    get("/items/health") {
        call.respond(mapOf("status" to "ok"))
    }

    get("/items/ready") {
        if (isReady()) {
            call.respond(mapOf("status" to "ready"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not ready"))
        }
    }
}
