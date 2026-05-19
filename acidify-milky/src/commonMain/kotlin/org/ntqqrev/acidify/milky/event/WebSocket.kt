package org.ntqqrev.acidify.milky.event

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.milky.MilkyContext

context(ctx: MilkyContext)
fun Route.eventWebSocket() = webSocket {
    val logger = ctx.bot.createLogger("WebSocketModule")
    logger.i { "${call.request.local.remoteAddress} 通过 WebSocket 连接" }
    launch {
        ctx.eventFlow.collect(::sendSerialized)
    }
    val reason = closeReason.await()
    if (reason?.code == CloseReason.Codes.NORMAL.code) {
        logger.i { "${call.request.local.remoteAddress} 断开 WebSocket 连接" }
    } else {
        logger.w { "${call.request.local.remoteAddress} 异常断开 WebSocket 连接 $reason" }
    }
}