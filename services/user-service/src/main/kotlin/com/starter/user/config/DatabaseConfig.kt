package com.starter.user.config

import com.starter.user.models.UserProfiles
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabaseConfig() {
    val dbConfig = environment.config.config("database")
    val host = dbConfig.property("host").getString()
    val port = dbConfig.property("port").getString()
    val name = dbConfig.property("name").getString()
    val user = dbConfig.property("user").getString()
    val password = dbConfig.property("password").getString()

    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$name"
        driverClassName = "org.postgresql.Driver"
        username = user
        this.password = password
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    Database.connect(dataSource)

    transaction {
        SchemaUtils.create(UserProfiles)
    }
}
