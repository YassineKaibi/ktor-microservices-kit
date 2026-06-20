package com.starter.crud.repository

import com.starter.crud.models.Item
import com.starter.crud.models.Items
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class PostgresItemRepository : ItemRepository {

    override suspend fun create(ownerId: Long, name: String, description: String?): Item =
        newSuspendedTransaction {
            val now = Instant.now()
            val id = UUID.randomUUID().toString()
            Items.insert {
                it[Items.id] = id
                it[Items.name] = name
                it[Items.description] = description
                it[Items.ownerId] = ownerId
                it[createdAt] = now
                it[updatedAt] = now
            }
            Item(id, name, description, ownerId, now, now)
        }

    override suspend fun findById(id: String, ownerId: Long): Item? = newSuspendedTransaction {
        Items.selectAll().where { (Items.id eq id) and (Items.ownerId eq ownerId) }
            .map { it.toItem() }
            .singleOrNull()
    }

    override suspend fun listByOwner(ownerId: Long, page: Int, size: Int): Pair<List<Item>, Long> =
        newSuspendedTransaction {
            val total = Items.selectAll().where { Items.ownerId eq ownerId }.count()
            val items = Items.selectAll().where { Items.ownerId eq ownerId }
                .orderBy(Items.createdAt to SortOrder.DESC)
                .limit(size, offset = ((page - 1).toLong() * size))
                .map { it.toItem() }
            items to total
        }

    override suspend fun update(id: String, ownerId: Long, name: String, description: String?): Item? =
        newSuspendedTransaction {
            val now = Instant.now()
            val updated = Items.update({ (Items.id eq id) and (Items.ownerId eq ownerId) }) {
                it[Items.name] = name
                it[Items.description] = description
                it[updatedAt] = now
            }
            if (updated == 0) {
                null
            } else {
                Items.selectAll().where { (Items.id eq id) and (Items.ownerId eq ownerId) }
                    .map { it.toItem() }
                    .single()
            }
        }

    override suspend fun delete(id: String, ownerId: Long): Boolean = newSuspendedTransaction {
        Items.deleteWhere { (Items.id eq id) and (Items.ownerId eq ownerId) } > 0
    }

    private fun ResultRow.toItem() = Item(
        id = this[Items.id],
        name = this[Items.name],
        description = this[Items.description],
        ownerId = this[Items.ownerId],
        createdAt = this[Items.createdAt],
        updatedAt = this[Items.updatedAt],
    )
}
