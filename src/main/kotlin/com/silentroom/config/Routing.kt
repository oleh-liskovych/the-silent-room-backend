package com.silentroom.config

import com.silentroom.routing.*
import com.silentroom.service.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.context.GlobalContext

fun Application.configureRouting() {
    val koin = GlobalContext.get()
    koin.loadModules(listOf(org.koin.dsl.module { single { WebSocketService() } }))

    val userService = koin.get<UserService>()
    val tokenService = koin.get<TokenService>()
    val contactService = koin.get<ContactService>()
    val chatService = koin.get<ChatService>()
    val webSocketService = koin.get<WebSocketService>()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "The Silent Room"))
        }

        userRoutes(userService)
        tokenRoutes(userService, tokenService)
        contactRoutes(contactService)
        chatRoutes(chatService)
        webSocketRoutes(chatService, webSocketService)
    }
}
