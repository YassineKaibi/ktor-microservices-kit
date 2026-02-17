package com.starter.auth.config

import io.ktor.server.application.*
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

fun Application.createRedisPool(): JedisPool {
    val redisConfig = environment.config.config("redis")
    val host = redisConfig.property("host").getString()
    val port = redisConfig.property("port").getString().toInt()

    return JedisPool(JedisPoolConfig(), host, port)
}
