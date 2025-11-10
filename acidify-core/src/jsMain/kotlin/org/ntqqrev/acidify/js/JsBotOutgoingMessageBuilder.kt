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

    fun mention(uin: JsNumber?, name: String) {
        underlying.mention(uin?.toLong(), name)
    }

    fun face(faceId: Int, isLarge: Boolean = false) {
        underlying.face(faceId, isLarge)
    }

    fun reply(sequence: JsNumber) {
        underlying.reply(sequence.toLong())
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
        duration: JsNumber
    ) {
        underlying.record(rawSilk, duration.toLong())
    }

    fun video(
        raw: ByteArray,
        width: Int,
        height: Int,
        duration: JsNumber,
        thumb: ByteArray,
        thumbFormat: ImageFormat
    ) {
        underlying.video(raw, width, height, duration.toLong(), thumb, thumbFormat)
    }

    fun forward(block: (JsBotForwardBlockBuilder) -> Promise<Unit>) {
        underlying.forward {
            val jsBuilder = JsBotForwardBlockBuilder(this)
            block(jsBuilder).await()
        }
    }
}