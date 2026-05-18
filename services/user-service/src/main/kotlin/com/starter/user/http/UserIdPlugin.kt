package com.starter.user.http

import com.starter.user.models.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*

private val UserIdKey = AttributeKey<Long>("UserId")

// Ktor plugin that reads `X-User-Id` (set by nginx after the auth_request
// subrequest succeeds) and stores it on the call. Routes that need an
// authenticated identity wrap themselves in `requireUserId { ... }`.
val RequireUserId = createRouteScopedPlugin("RequireUserId") {
    onCall { call ->
        val header = call.request.header("X-User-Id")
        val userId = header?.toLongOrNull()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid X-User-Id"))
            return@onCall
        }
        call.attributes.put(UserIdKey, userId)
    }
}

val ApplicationCall.userId: Long
    get() = attributes[UserIdKey]
