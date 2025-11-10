package org.ntqqrev.acidify.js

import kotlinx.coroutines.await
import org.ntqqrev.acidify.message.BotOutgoingMessageBuilder
import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.message.ImageSubType
import kotlin.js.Promise

@JsExport
@JsName("BotOutgoingMessageBuilder")
class JsBotOutgoingMessageBuilder internal constructor(
    private val underlying: BotOutgoingMessageBuilder
) {
    fun text(text: String) {
        underlying.text(text)
    }

    fun mention(uin: Long?, name: String) {
        underlying.mention(uin, name)
    }

    fun face(faceId: Int, isLarge: Boolean = false) {
        underlying.face(faceId, isLarge)
    }

    fun reply(sequence: Long) {
        underlying.reply(sequence)
    }

    fun image(
        raw: ByteArray,
        format: ImageFormat,
        width: Int,
        height: Int,
        subType: ImageSubType = ImageSubType.NORMAL,
        summary: String = "[图片]"
    ) {
        underlying.image(raw, format, width, height, subType, summary)
    }

    fun record(
        rawSilk: ByteArray,
        duration: Long
    ) {
        underlying.record(rawSilk, duration)
    }

    fun video(
        raw: ByteArray,
        width: Int,
        height: Int,
        duration: Long,
        thumb: ByteArray,
        thumbFormat: ImageFormat
    ) {
        underlying.video(raw, width, height, duration, thumb, thumbFormat)
    }

    fun forward(block: (JsBotForwardBlockBuilder) -> Promise<Unit>) {
        underlying.forward {
            val jsBuilder = JsBotForwardBlockBuilder(this)
            block(jsBuilder).await()
        }
    }
}