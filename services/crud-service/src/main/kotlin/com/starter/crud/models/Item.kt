package com.starter.crud.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Generic template entity. `ownerId` is the auth user id supplied by the gateway via the
 * X-User-Id header; every operation is scoped to the calling owner.
 */
object Items : Table("items") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val ownerId = long("owner_id")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

data class Item(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class ItemResponse(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: Long,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val description: String? = null,
)

@Serializable
data class UpdateItemRequest(
    val name: String,
    val description: String? = null,
)

@Serializable
data class PagedItemsResponse(
    val data: List<ItemResponse>,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalItems: Long,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

fun Item.toResponse() = ItemResponse(
    id = id,
    name = name,
    description = description,
    ownerId = ownerId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
