package org.ntqqrev.acidify.message

import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.internal.packet.message.CommonMessage
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.message.internal.MessageParsingContext
import kotlin.js.JsExport

/**
 * 合并转发消息中的单条消息
 * @property senderName 发送者名称
 * @property avatarUrl 发送者头像 URL
 * @property timestamp 消息发送的 Unix 时间戳（秒）
 * @property segments 消息内容
 */
@JsExport
class BotForwardedMessage(
    val senderName: String,
    val avatarUrl: String,
    val timestamp: Long,
) {
    internal val segmentsMut = mutableListOf<BotIncomingSegment>()
    val segments: List<BotIncomingSegment>
        get() = segmentsMut

    companion object {
        internal fun Bot.parseForwardedMessage(raw: PbObject<CommonMessage>): BotForwardedMessage? {
            val routingHead = raw.get { routingHead }
            val contentHead = raw.get { contentHead }
            val elems = raw.get { messageBody }.get { richText }.get { elems }
            val senderName = routingHead.get { commonC2C }.get { name }.takeIf { it.isNotEmpty() }
                ?: routingHead.get { group }.get { groupCard }.takeIf { it.isNotEmpty() }
                ?: "QQ用户"
            val avatarUrl = contentHead.get { forwardExt }.get { avatar }
            val message = BotForwardedMessage(
                senderName = senderName,
                avatarUrl = avatarUrl,
                timestamp = contentHead.get { time }
            )

            // 创建一个临时的 BotIncomingMessage 用于解析消息段
            val tempMessage = BotIncomingMessage(
                scene = MessageScene.FRIEND,
                peerUin = 0L,
                peerUid = "",
                sequence = contentHead.get { sequence },
                timestamp = contentHead.get { time },
                senderUin = routingHead.get { fromUin },
                senderUid = routingHead.get { fromUid },
                clientSequence = 0L,
                random = 0,
                messageUid = contentHead.get { msgUid },
            )

            val ctx = MessageParsingContext(tempMessage, elems, this)
            while (ctx.hasNext()) {
                var matched = false
                for (factory in BotIncomingMessage.factories) {
                    val segment = factory.tryParse(ctx) ?: continue
                    message.segmentsMut += segment
                    matched = true
                    break
                }
                if (!matched) {
                    ctx.skip()
                }
            }

            return message.takeIf { it.segments.isNotEmpty() }
        }
    }
}

