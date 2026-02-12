package com.starter.auth

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/auth/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }
}
