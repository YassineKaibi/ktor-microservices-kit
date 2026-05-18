package com.starter.user

import com.starter.user.config.configureDatabaseConfig
import com.starter.user.models.ErrorResponse
import com.starter.user.models.ProfileDeletedException
import com.starter.user.models.ProfileNotFoundException
import com.starter.user.models.ProfileValidationException
import com.starter.user.repository.PostgresUserProfileRepository
import com.starter.user.routes.userRoutes
import com.starter.user.services.UserProfileService
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
        exception<ProfileValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Validation failed"))
        }
        exception<ProfileNotFoundException> { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Profile not found"))
        }
        exception<ProfileDeletedException> { call, _ ->
            call.respond(HttpStatusCode.Gone, ErrorResponse("Profile has been deleted"))
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }

    configureDatabaseConfig()

    val repository = PostgresUserProfileRepository()
    val service = UserProfileService(repository)

    routing {
        userRoutes(service)
    }
}
