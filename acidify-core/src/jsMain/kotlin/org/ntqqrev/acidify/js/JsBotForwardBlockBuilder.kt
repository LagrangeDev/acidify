package org.ntqqrev.acidify.js

import org.ntqqrev.acidify.message.BotOutgoingMessageBuilder
import kotlin.time.Clock

@JsExport
@JsName("BotForwardBlockBuilder")
@AcidifyJsWrapper
class JsBotForwardBlockBuilder internal constructor(
    val underlying: BotOutgoingMessageBuilder.Forward
) {
    fun node(
        senderUin: Long,
        senderName: String,
        block: (JsBotOutgoingMessageBuilder) -> Unit,
        timestamp: Long = Clock.System.now().epochSeconds
    ) {
        underlying.node(
            senderUin,
            senderName,
            timestamp,
        ) {
            val b = JsBotOutgoingMessageBuilder(this)
            block(b)
        }
    }
}