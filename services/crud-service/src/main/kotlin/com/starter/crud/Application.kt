package com.starter.crud

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
        get("/items/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }
}
