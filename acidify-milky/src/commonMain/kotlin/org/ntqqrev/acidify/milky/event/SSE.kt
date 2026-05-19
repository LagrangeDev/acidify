package org.ntqqrev.acidify.milky.event

import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.milky.milkyJsonModule

context(ctx: MilkyContext)
fun Route.eventSse() = sse {
    val logger = ctx.bot.createLogger("SseModule")
    logger.i { "${call.request.local.remoteAddress} 通过 SSE 连接" }
    launch {
        ctx.eventFlow.collect {
            send(
                data = milkyJsonModule.encodeToString(it),
                event = "milky_event"
            )
        }
    }
}