package com.starter.crud.repository

import com.starter.crud.models.Item

/** All operations are scoped to the owning user (the X-User-Id from the gateway). */
interface ItemRepository {
    suspend fun create(ownerId: Long, name: String, description: String?): Item

    suspend fun findById(id: String, ownerId: Long): Item?

    /** One page of the owner's items plus the owner's total item count. */
    suspend fun listByOwner(ownerId: Long, page: Int, size: Int): Pair<List<Item>, Long>

    suspend fun update(id: String, ownerId: Long, name: String, description: String?): Item?

    suspend fun delete(id: String, ownerId: Long): Boolean
}
