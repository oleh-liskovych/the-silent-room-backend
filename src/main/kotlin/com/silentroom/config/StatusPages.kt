package com.silentroom.config

import com.silentroom.dto.response.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    val logger = log
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", cause.message ?: "Bad request"))
        }
        exception<IllegalStateException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("conflict", cause.message ?: "Conflict"))
        }
        exception<Exception> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "An internal error occurred"))
        }
    }
}
