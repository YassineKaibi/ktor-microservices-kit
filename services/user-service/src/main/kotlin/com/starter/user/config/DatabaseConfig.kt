package com.starter.user.config

import com.starter.user.models.UserProfiles
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Owns the Postgres connection and gates readiness on it.
 *
 * The pod must stay alive (and serve liveness) even when Postgres is not up yet, so the
 * initial connect runs in the background with capped exponential backoff and never hard-exits.
 * Readiness flips to true only once the pool is live and the schema is created, and each
 * [isReady] call re-validates the connection so a datastore that drops out fails readiness.
 */
class DatabaseManager(
    private val host: String,
    private val port: String,
    private val name: String,
    private val user: String,
    private val password: String,
) {
    private val log = LoggerFactory.getLogger(DatabaseManager::class.java)

    @Volatile
    private var dataSource: HikariDataSource? = null

    /** Launches the connect-with-retry loop on the application's scope; returns immediately. */
    fun connect(application: Application) {
        application.launch(Dispatchers.IO) {
            var backoffMs = 1_000L
            while (true) {
                try {
                    val ds = HikariDataSource(HikariConfig().apply {
                        jdbcUrl = "jdbc:postgresql://$host:$port/$name"
                        driverClassName = "org.postgresql.Driver"
                        username = user
                        this.password = this@DatabaseManager.password
                        maximumPoolSize = 10
                        isAutoCommit = false
                        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                        connectionTimeout = 5_000
                        validate()
                    })
                    Database.connect(ds)
                    transaction { SchemaUtils.create(UserProfiles) }
                    dataSource = ds
                    log.info("Connected to Postgres at {}:{}/{} and ensured schema", host, port, name)
                    return@launch
                } catch (e: Exception) {
                    log.warn("Postgres not ready ({}); retrying in {}ms", e.message, backoffMs)
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                }
            }
        }
    }

    /** True only when the datastore is reachable right now. */
    suspend fun isReady(): Boolean {
        val ds = dataSource ?: return false
        return withContext(Dispatchers.IO) {
            try {
                ds.connection.use { it.isValid(2) }
            } catch (e: Exception) {
                false
            }
        }
    }
}
