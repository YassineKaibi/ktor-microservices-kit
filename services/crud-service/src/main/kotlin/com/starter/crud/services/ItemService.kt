package com.starter.crud.services

import com.starter.crud.models.Item
import com.starter.crud.repository.ItemRepository

class ItemService(
    private val itemRepository: ItemRepository,
) {
    data class Page(
        val items: List<Item>,
        val page: Int,
        val size: Int,
        val totalPages: Int,
        val totalItems: Long,
    )

    suspend fun create(ownerId: Long, name: String, description: String?): Item =
        itemRepository.create(ownerId, name, description)

    suspend fun get(id: String, ownerId: Long): Item? = itemRepository.findById(id, ownerId)

    suspend fun list(ownerId: Long, page: Int, size: Int): Page {
        val (items, total) = itemRepository.listByOwner(ownerId, page, size)
        val totalPages = if (total == 0L) 0 else ((total + size - 1) / size).toInt()
        return Page(items, page, size, totalPages, total)
    }

    suspend fun update(id: String, ownerId: Long, name: String, description: String?): Item? =
        itemRepository.update(id, ownerId, name, description)

    suspend fun delete(id: String, ownerId: Long): Boolean = itemRepository.delete(id, ownerId)
}
