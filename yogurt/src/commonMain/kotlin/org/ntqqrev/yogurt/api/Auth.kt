package org.ntqqrev.yogurt.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ntqqrev.yogurt.YogurtApp.config

fun Route.configureMilkyApiAuth() = install(createRouteScopedPlugin("ApiAuth") {
    onCall { call ->
        if (call.request.headers["Authorization"] != "Bearer ${config.milky.http.accessToken}") {
            call.respond(HttpStatusCode.Unauthorized)
            return@onCall
        }
    }
})