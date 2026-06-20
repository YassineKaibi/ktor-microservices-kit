package com.starter.crud

import com.starter.crud.config.DatabaseManager
import com.starter.crud.models.ErrorResponse
import com.starter.crud.repository.InMemoryItemRepository
import com.starter.crud.repository.ItemRepository
import com.starter.crud.repository.PostgresItemRepository
import com.starter.crud.routes.healthRoutes
import com.starter.crud.routes.itemRoutes
import com.starter.crud.services.ItemService
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

    val repository: ItemRepository
    val readinessCheck: suspend () -> Boolean

    if (backend == "memory") {
        log.info("Using in-memory repository backend (no external datastore)")
        repository = InMemoryItemRepository()
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
        repository = PostgresItemRepository()
        readinessCheck = { dbManager.isReady() }
    }

    val itemService = ItemService(repository)

    routing {
        healthRoutes(readinessCheck)
        itemRoutes(itemService)
    }
}
