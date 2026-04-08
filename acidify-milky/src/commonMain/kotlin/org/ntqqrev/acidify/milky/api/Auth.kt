package org.ntqqrev.acidify.milky.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.milky.MilkyContext

context(ctx: MilkyContext)
fun Route.apiAuth() = install(createRouteScopedPlugin("ApiAuth") {
    onCall { call ->
        val logger = ctx.bot.createLogger("ApiAuthPlugin")
        if (call.request.headers["Authorization"] != "Bearer ${ctx.httpAccessToken}") {
            logger.i { "${call.request.local.remoteAddress} 未携带正确的 access token，拒绝访问" }
            call.respond(HttpStatusCode.Unauthorized)
            return@onCall
        }
    }
})