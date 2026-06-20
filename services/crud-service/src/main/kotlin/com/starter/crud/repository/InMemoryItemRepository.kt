package com.starter.crud.repository

import com.starter.crud.models.Item
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process backend selected via REPO_BACKEND=memory so the stack runs with no external DB.
 * State is per-process and not shared across replicas — intended for local runs and tests.
 */
class InMemoryItemRepository : ItemRepository {

    private val items = ConcurrentHashMap<String, Item>()

    override suspend fun create(ownerId: Long, name: String, description: String?): Item {
        val now = Instant.now()
        val item = Item(UUID.randomUUID().toString(), name, description, ownerId, now, now)
        items[item.id] = item
        return item
    }

    override suspend fun findById(id: String, ownerId: Long): Item? =
        items[id]?.takeIf { it.ownerId == ownerId }

    override suspend fun listByOwner(ownerId: Long, page: Int, size: Int): Pair<List<Item>, Long> {
        val owned = items.values
            .filter { it.ownerId == ownerId }
            .sortedByDescending { it.createdAt }
        val pageItems = owned.drop((page - 1) * size).take(size)
        return pageItems to owned.size.toLong()
    }

    override suspend fun update(id: String, ownerId: Long, name: String, description: String?): Item? {
        val existing = items[id] ?: return null
        if (existing.ownerId != ownerId) return null
        val updated = existing.copy(name = name, description = description, updatedAt = Instant.now())
        items[id] = updated
        return updated
    }

    override suspend fun delete(id: String, ownerId: Long): Boolean {
        val existing = items[id] ?: return false
        if (existing.ownerId != ownerId) return false
        items.remove(id)
        return true
    }
}
