package org.ntqqrev.acidify.message

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.ntqqrev.acidify.internal.packet.misc.GroupEssenceMsgItem
import kotlin.js.JsExport

/**
 * 群精华消息
 * @property groupUin 群号
 * @property messageSeq 消息序列号
 * @property messageTime 消息发送时的 Unix 时间戳（秒）
 * @property senderUin 发送者 QQ 号
 * @property senderName 发送者名称
 * @property operatorUin 设置精华的操作者 QQ 号
 * @property operatorName 设置精华的操作者名称
 * @property operationTime 消息被设置精华时的 Unix 时间戳（秒）
 * @property segments 消息段列表
 */
@JsExport
class BotEssenceMessage(
    val groupUin: Long,
    val messageSeq: Long,
    val messageTime: Long,
    val senderUin: Long,
    val senderName: String,
    val operatorUin: Long,
    val operatorName: String,
    val operationTime: Long,
    val segments: List<BotEssenceSegment>
) {
    companion object {
        internal fun GroupEssenceMsgItem.toBotEssenceMessage(groupUin: Long) = BotEssenceMessage(
            groupUin = groupUin,
            messageSeq = this.msgSeq,
            messageTime = this.senderTime,
            senderUin = this.senderUin.toLongOrNull() ?: 0L,
            senderName = this.senderNick,
            operatorUin = this.addDigestUin.toLongOrNull() ?: 0L,
            operatorName = this.addDigestNick,
            operationTime = this.addDigestTime,
            segments = this.msgContent.mapNotNull { element ->
                val msgType = element["msg_type"]?.jsonPrimitive?.int ?: return@mapNotNull null
                when (msgType) {
                    1 -> BotEssenceSegment.Text(element["text"]?.jsonPrimitive?.content ?: "")
                    2 -> BotEssenceSegment.Face(element["face_index"]?.jsonPrimitive?.int ?: 0)
                    3 -> BotEssenceSegment.Image(element["image_url"]?.jsonPrimitive?.content ?: "")
                    4 -> BotEssenceSegment.Video(element["file_thumbnail_url"]?.jsonPrimitive?.content ?: "")
                    else -> null
                }
            }
        ).takeIf { it.segments.isNotEmpty() }
    }
}