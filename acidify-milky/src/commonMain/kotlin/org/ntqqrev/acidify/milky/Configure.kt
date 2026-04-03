package org.ntqqrev.acidify.milky

import io.ktor.server.routing.*
import org.ntqqrev.acidify.milky.api.apiAuth
import org.ntqqrev.acidify.milky.api.apiLoginProtect
import org.ntqqrev.acidify.milky.api.apiRoutes
import org.ntqqrev.acidify.milky.event.eventAuth
import org.ntqqrev.acidify.milky.event.eventSse
import org.ntqqrev.acidify.milky.event.eventWebSocket
import org.ntqqrev.acidify.milky.event.eventWebhook

context(ctx: MilkyContext)
fun Route.configureMilky() {
    route("/api") {
        if (ctx.httpAccessToken.isNotEmpty()) {
            apiAuth()
        }
        apiLoginProtect()
        apiRoutes()
    }
    route("/event") {
        if (ctx.httpAccessToken.isNotEmpty()) {
            eventAuth()
        }
        eventWebSocket()
        eventSse()
    }
    with(application) {
        eventWebhook()
    }
}