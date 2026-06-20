package com.starter.user

import com.starter.user.config.DatabaseManager
import com.starter.user.models.ErrorResponse
import com.starter.user.repository.InMemoryUserRepository
import com.starter.user.repository.PostgresUserRepository
import com.starter.user.repository.UserRepository
import com.starter.user.routes.healthRoutes
import com.starter.user.routes.userRoutes
import com.starter.user.services.UserService
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

    val backend = environment.config.property("repository.backend").getString()

    val repository: UserRepository
    val readinessCheck: suspend () -> Boolean

    if (backend == "memory") {
        log.info("Using in-memory repository backend (no external datastore)")
        repository = InMemoryUserRepository()
        readinessCheck = { true }
    } else {
        val dbConfig = environment.config.config("database")
        val dbManager = DatabaseManager(
            host = dbConfig.property("host").getString(),
            port = dbConfig.property("port").getString(),
            name = dbConfig.property("name").getString(),
            user = dbConfig.property("user").getString(),
            password = dbConfig.property("password").getString(),
        )
        dbManager.connect(this)
        repository = PostgresUserRepository()
        readinessCheck = { dbManager.isReady() }
    }

    val userService = UserService(repository)

    routing {
        healthRoutes(readinessCheck)
        userRoutes(userService)
    }
}
