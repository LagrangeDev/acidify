package org.ntqqrev.acidify.js

import kotlinx.coroutines.await
import org.ntqqrev.acidify.message.BotForwardBlockBuilder
import kotlin.js.Promise

@JsExport
@JsName("BotForwardBlockBuilder")
class JsBotForwardBlockBuilder internal constructor(
    val underlying: BotForwardBlockBuilder
) {
    fun node(
        senderUin: Long,
        senderName: String,
        block: (JsBotOutgoingMessageBuilder) -> Promise<Unit>
    ) {
        underlying.node(
            senderUin,
            senderName
        ) {
            val b = JsBotOutgoingMessageBuilder(this)
            block(b).await()
        }
    }
}