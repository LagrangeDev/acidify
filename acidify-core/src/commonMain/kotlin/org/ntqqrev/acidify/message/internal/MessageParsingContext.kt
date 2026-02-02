package org.ntqqrev.acidify.message.internal

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.internal.json.GroupEssenceMsgItem
import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import org.ntqqrev.acidify.internal.proto.message.Elem
import org.ntqqrev.acidify.internal.proto.message.PushMsgType
import org.ntqqrev.acidify.internal.proto.message.extra.PrivateFileExtra
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.message.*
import org.ntqqrev.acidify.message.BotIncomingMessage.ExtraInfo

internal class MessageParsingContext(
    val scene: MessageScene,
    val elems: List<Elem>,
    val bot: Bot,
) {
    var currentIndex = 0

    val remainingCount: Int
        get() = elems.size - currentIndex

    fun hasNext(): Boolean = currentIndex < elems.size

    fun peek(): Elem = elems[currentIndex]

    inline fun <T> tryPeekType(typeProvider: Elem.() -> T?): T? = peek().typeProvider()

    fun skip(count: Int = 1) {
        if (currentIndex + count > elems.size) {
            throw IndexOutOfBoundsException("Cannot skip $count elements from index $currentIndex, size is ${elems.size}")
        }
        currentIndex += count
    }

    fun consume() = skip(1)

    companion object {
        internal val factories = listOf(
            IncomingSegmentFactory.Text,
            IncomingSegmentFactory.Mention,
            IncomingSegmentFactory.Face,
            IncomingSegmentFactory.Reply,
            IncomingSegmentFactory.Image,
            IncomingSegmentFactory.Record,
            IncomingSegmentFactory.Video,
            IncomingSegmentFactory.File,
            IncomingSegmentFactory.Forward,
            IncomingSegmentFactory.MarketFace,
            IncomingSegmentFactory.LightApp,
        )

        internal fun Bot.parseMessage(raw: CommonMessage): BotIncomingMessage? {
            val contentHead = raw.contentHead
            val pushMsgType = PushMsgType.from(contentHead.type) ?: return null
            val draftMsg = buildDraftMessage(raw, pushMsgType) ?: return null
            val segments = if (pushMsgType != PushMsgType.FriendFileMessage) {
                buildSegments(raw.messageBody.richText.elems, draftMsg.scene) {
                    draftMsg.extraInfo = it
                }
            } else {
                val notOnlineFile = raw.messageBody.msgContent.pbDecode<PrivateFileExtra>().notOnlineFile
                listOf(
                    BotIncomingSegment.File(
                        fileId = notOnlineFile.fileUuid,
                        fileName = notOnlineFile.fileName,
                        fileSize = notOnlineFile.fileSize,
                        fileHash = notOnlineFile.fileIdCrcMedia
                    )
                )
            }

            if (segments.isEmpty()) {
                return null
            }

            draftMsg.segments = segments
            return draftMsg
        }

        private fun Bot.buildDraftMessage(raw: CommonMessage, pushMsgType: PushMsgType): BotIncomingMessage? {
            val routingHead = raw.routingHead
            val contentHead = raw.contentHead

            return when (pushMsgType) {
                PushMsgType.FriendMessage,
                PushMsgType.FriendRecordMessage,
                PushMsgType.FriendFileMessage -> {
                    val isSelfSend = routingHead.fromUin == this.uin
                    BotIncomingMessage(
                        scene = MessageScene.FRIEND,
                        peerUin = if (isSelfSend) routingHead.toUin else routingHead.fromUin,
                        peerUid = if (isSelfSend) routingHead.toUid else routingHead.fromUid,
                        sequence = contentHead.clientSequence,
                        timestamp = contentHead.time,
                        senderUin = routingHead.fromUin,
                        senderUid = routingHead.fromUid,
                        clientSequence = contentHead.sequence, // weird
                        random = contentHead.random,
                        messageUid = contentHead.msgUid,
                        raw = raw,
                    )
                }

                PushMsgType.GroupMessage -> BotIncomingMessage(
                    scene = MessageScene.GROUP,
                    peerUin = routingHead.group.groupCode,
                    peerUid = routingHead.toUid,
                    sequence = contentHead.sequence,
                    timestamp = contentHead.time,
                    senderUin = routingHead.fromUin,
                    senderUid = routingHead.fromUid,
                    clientSequence = contentHead.clientSequence,
                    random = contentHead.random,
                    messageUid = contentHead.msgUid,
                    raw = raw,
                )

                else -> null
            }
        }

        internal fun Bot.buildSegments(
            elems: List<Elem>,
            scene: MessageScene,
            onExtraInfo: ((ExtraInfo) -> Unit)? = null
        ): List<BotIncomingSegment> {
            val segments = mutableListOf<BotIncomingSegment>()
            val ctx = MessageParsingContext(scene, elems, this)
            while (ctx.hasNext()) {
                ctx.tryPeekType { this.extraInfo }?.let {
                    onExtraInfo?.invoke(
                        ExtraInfo(
                            nick = it.nick,
                            groupCard = it.groupCard,
                            specialTitle = it.senderTitle,
                        )
                    )
                    ctx.consume()
                    continue
                }

                var matched = false
                for (factory in factories) {
                    val segment = factory.tryParse(ctx) ?: continue
                    segments += segment
                    matched = true
                    break
                }
                if (!matched) {
                    ctx.skip()
                }
            }

            return segments
        }

        internal fun Bot.parseForwardedMessage(raw: CommonMessage): BotForwardedMessage? {
            val routingHead = raw.routingHead
            val contentHead = raw.contentHead
            val senderName = routingHead.commonC2C.name.takeIf { it.isNotEmpty() }
                ?: routingHead.group.groupCard.takeIf { it.isNotEmpty() }
                ?: "QQ用户"
            val avatarUrl = contentHead.forwardExt.avatar
            val message = BotForwardedMessage(
                senderName = senderName,
                avatarUrl = avatarUrl,
                timestamp = contentHead.time
            )
            val segments = buildSegments(
                elems = raw.messageBody.richText.elems,
                scene = MessageScene.FRIEND
            )
            if (segments.isEmpty()) {
                return null
            }
            message.segments = segments
            return message
        }

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
