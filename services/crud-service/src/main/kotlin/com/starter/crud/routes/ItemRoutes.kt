package com.starter.crud.routes

import com.starter.crud.models.CreateItemRequest
import com.starter.crud.models.ErrorResponse
import com.starter.crud.models.PagedItemsResponse
import com.starter.crud.models.UpdateItemRequest
import com.starter.crud.models.toResponse
import com.starter.crud.services.ItemService
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

fun Route.itemRoutes(itemService: ItemService) {
    get("/items") {
        val ownerId = call.requireUserId() ?: return@get
        val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val size = (call.request.queryParameters["size"]?.toIntOrNull() ?: 20).coerceIn(1, 100)

        val result = itemService.list(ownerId, page, size)
        call.respond(
            PagedItemsResponse(
                data = result.items.map { it.toResponse() },
                page = result.page,
                size = result.size,
                totalPages = result.totalPages,
                totalItems = result.totalItems,
            )
        )
    }

    post("/items") {
        val ownerId = call.requireUserId() ?: return@post
        val request = call.receive<CreateItemRequest>()
        if (request.name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Name is required"))
            return@post
        }
        val item = itemService.create(ownerId, request.name, request.description)
        call.respond(HttpStatusCode.Created, item.toResponse())
    }

    get("/items/{id}") {
        val ownerId = call.requireUserId() ?: return@get
        val id = call.parameters["id"]!!
        val item = itemService.get(id, ownerId)
        if (item == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Item not found"))
        } else {
            call.respond(item.toResponse())
        }
    }

    put("/items/{id}") {
        val ownerId = call.requireUserId() ?: return@put
        val id = call.parameters["id"]!!
        val request = call.receive<UpdateItemRequest>()
        if (request.name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Name is required"))
            return@put
        }
        val item = itemService.update(id, ownerId, request.name, request.description)
        if (item == null) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Item not found"))
        } else {
            call.respond(item.toResponse())
        }
    }

    delete("/items/{id}") {
        val ownerId = call.requireUserId() ?: return@delete
        val id = call.parameters["id"]!!
        if (itemService.delete(id, ownerId)) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Item not found"))
        }
    }
}
