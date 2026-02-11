package org.ntqqrev.acidify

import org.ntqqrev.acidify.exception.MessageSendException
import org.ntqqrev.acidify.internal.proto.message.media.FileId
import org.ntqqrev.acidify.internal.proto.message.media.IndexNode
import org.ntqqrev.acidify.internal.service.group.FetchGroupExtraInfo
import org.ntqqrev.acidify.internal.service.message.*
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.message.*
import org.ntqqrev.acidify.message.internal.MessageBuildingContext
import org.ntqqrev.acidify.message.internal.parseForwardedMessage
import org.ntqqrev.acidify.message.internal.parseMessage
import kotlin.io.encoding.Base64
import kotlin.random.Random

/**
 * 发送好友消息
 * @param friendUin 好友 QQ 号
 * @param clientSequence 客户端消息序列号，默认随机生成，可用于 IM 开发
 * @param random 消息随机数，默认随机生成，可用于 IM 开发
 * @param segments 消息段列表
 */
suspend fun AbstractBot.sendFriendMessage(
    friendUin: Long,
    clientSequence: Long = Random.nextLong(),
    random: Int = Random.nextInt(),
    segments: List<BotOutgoingSegment>,
): BotOutgoingMessageResult {
    val friendUid = getUidByUin(friendUin)
    val elems = MessageBuildingContext(
        bot = this,
        scene = MessageScene.FRIEND,
        peerUin = friendUin,
        peerUid = friendUid,
        segments = segments,
    ).build()
    val resp = client.callService(
        SendFriendMessage,
        SendFriendMessage.Req(
            friendUin,
            friendUid,
            elems,
            clientSequence,
            random
        )
    )
    if (resp.result != 0) {
        throw MessageSendException(resp.result, resp.errMsg)
    }
    return BotOutgoingMessageResult(resp.sequence, resp.sendTime)
}

/**
 * 发送好友消息
 * @param friendUin 好友 QQ 号
 * @param clientSequence 客户端消息序列号，默认随机生成，可用于 IM 开发
 * @param random 消息随机数，默认随机生成，可用于 IM 开发
 * @param builderAction 消息段构建器
 */
suspend inline fun AbstractBot.sendFriendMessage(
    friendUin: Long,
    clientSequence: Long = Random.nextLong(),
    random: Int = Random.nextInt(),
    builderAction: BotOutgoingMessageBuilder.() -> Unit,
) = sendFriendMessage(
    friendUin = friendUin,
    clientSequence = clientSequence,
    random = random,
    segments = BotOutgoingMessageBuilder().apply(builderAction).segments
)

/**
 * 发送群消息
 * @param groupUin 群号
 * @param clientSequence 客户端消息序列号，默认随机生成，可用于 IM 开发
 * @param random 消息随机数，默认随机生成，可用于 IM 开发
 * @param segments 消息段列表
 */
suspend fun AbstractBot.sendGroupMessage(
    groupUin: Long,
    clientSequence: Long = Random.nextLong(),
    random: Int = Random.nextInt(),
    segments: List<BotOutgoingSegment>,
): BotOutgoingMessageResult {
    val elems = MessageBuildingContext(
        bot = this,
        scene = MessageScene.GROUP,
        peerUin = groupUin,
        peerUid = groupUin.toString(),
        segments = segments,
    ).build()
    val resp = client.callService(
        SendGroupMessage,
        SendGroupMessage.Req(
            groupUin,
            elems,
            clientSequence,
            random
        )
    )
    if (resp.result != 0) {
        throw MessageSendException(resp.result, resp.errMsg)
    }
    return BotOutgoingMessageResult(resp.sequence, resp.sendTime)
}

/**
 * 发送群消息
 * @param groupUin 群号
 * @param clientSequence 客户端消息序列号，默认随机生成，可用于 IM 开发
 * @param random 消息随机数，默认随机生成，可用于 IM 开发
 * @param builderAction 消息段构建器
 */
suspend inline fun AbstractBot.sendGroupMessage(
    groupUin: Long,
    clientSequence: Long = Random.nextLong(),
    random: Int = Random.nextInt(),
    builderAction: BotOutgoingMessageBuilder.() -> Unit,
) = sendGroupMessage(
    groupUin = groupUin,
    clientSequence = clientSequence,
    random = random,
    segments = BotOutgoingMessageBuilder().apply(builderAction).segments
)

/**
 * 撤回好友消息
 * @param friendUin 好友 QQ 号
 * @param sequence 消息序列号
 */
suspend fun AbstractBot.recallFriendMessage(
    friendUin: Long,
    sequence: Long
) {
    val friendUid = getUidByUin(friendUin)

    // 从原始消息包中提取 random 字段
    val raw = client.callService(
        FetchFriendMessages,
        FetchFriendMessages.Req(
            friendUid,
            sequence,
            sequence
        )
    ).firstOrNull() ?: throw IllegalStateException("消息不存在")

    val contentHead = raw.contentHead
    val random = contentHead.random
    val timestamp = contentHead.time
    val privateSequence = contentHead.sequence

    client.callService(
        RecallFriendMessage,
        RecallFriendMessage.Req(
            targetUid = friendUid,
            clientSequence = privateSequence,
            messageSequence = sequence,
            random = random,
            timestamp = timestamp
        )
    )
}

/**
 * 撤回群消息
 * @param groupUin 群号
 * @param sequence 消息序列号
 */
suspend fun AbstractBot.recallGroupMessage(groupUin: Long, sequence: Long) = client.callService(
    RecallGroupMessage,
    RecallGroupMessage.Req(
        groupUin = groupUin, sequence = sequence
    )
)

/**
 * 向上获取与好友的历史消息
 * @param friendUin 好友 QQ 号
 * @param limit 最多获取的消息数量，最大值为 30
 * @param startSequence 起始消息序列号（包含该序列号），为 `null` 则从最新消息开始获取
 */
suspend fun AbstractBot.getFriendHistoryMessages(
    friendUin: Long,
    limit: Int,
    startSequence: Long? = null
): BotHistoryMessages {
    require(limit in 1..30) { "limit 必须在 1 到 30 之间" }
    val friendUid = getUidByUin(friendUin)
    val end = startSequence ?: client.callService(GetFriendLatestSequence, friendUid)
    val start = (end - limit + 1).coerceAtLeast(1)

    val resp = client.callService(
        FetchFriendMessages,
        FetchFriendMessages.Req(friendUid, start, end)
    )

    val messages = resp.mapNotNull { parseMessage(it) }

    val nextStartSeq = if (start > 1) (start - 1) else null
    return BotHistoryMessages(messages, nextStartSeq)
}

/**
 * 向上获取群聊的历史消息
 * @param groupUin 群号
 * @param limit 最多获取的消息数量，最大值为 30
 * @param startSequence 起始消息序列号（包含该序列号），为 `null` 则从最新消息开始获取
 */
suspend fun AbstractBot.getGroupHistoryMessages(
    groupUin: Long,
    limit: Int,
    startSequence: Long? = null
): BotHistoryMessages {
    require(limit in 1..30) { "limit 必须在 1 到 30 之间" }
    val end = startSequence ?: client.callService(FetchGroupExtraInfo, groupUin).latestMessageSeq
    val start = (end - limit + 1).coerceAtLeast(1)

    val resp = client.callService(
        FetchGroupMessages,
        FetchGroupMessages.Req(groupUin, start, end)
    )

    val messages = resp.mapNotNull { parseMessage(it) }

    val nextStartSeq = if (start > 1) (start - 1) else null
    return BotHistoryMessages(messages, nextStartSeq)
}

/**
 * 获取给定资源 ID 的下载链接，支持图片、语音、视频。
 */
suspend fun AbstractBot.getDownloadUrl(resourceId: String): String {
    if (resourceId.startsWith("http://") || resourceId.startsWith("https://"))
        return resourceId // direct URL

    val actualLength = if (resourceId.length % 4 == 0) {
        resourceId.length
    } else {
        resourceId.length + (4 - resourceId.length % 4)
    }
    val normalizedBase64 = resourceId
        .replace("-", "+")
        .replace("_", "/")
        .padEnd(actualLength, '=')
    val fileIdDecoded = Base64.decode(normalizedBase64).pbDecode<FileId>()
    val appId = fileIdDecoded.appId
    val indexNode = IndexNode(
        fileUuid = resourceId,
        storeId = fileIdDecoded.storeId,
        ttl = fileIdDecoded.ttl,
    )
    return client.callService(
        when (appId) {
            1402 -> RichMediaDownload.PrivateRecord
            1403 -> RichMediaDownload.GroupRecord
            1406 -> RichMediaDownload.PrivateImage
            1407 -> RichMediaDownload.GroupImage
            1413 -> RichMediaDownload.PrivateVideo
            1415 -> RichMediaDownload.GroupVideo
            else -> throw IllegalArgumentException("不支持的资源类型 $appId")
        },
        indexNode
    )
}

/**
 * 获取合并转发消息内容
 * @param resId 合并转发消息的 resId
 * @return 转发消息列表
 */
suspend fun AbstractBot.getForwardedMessages(resId: String): List<BotForwardedMessage> {
    return client.callService(RecvLongMsg, RecvLongMsg.Req(resId))
        .mapNotNull { parseForwardedMessage(it) }
}

/**
 * 标记好友消息为已读
 * @param friendUin 好友 QQ 号
 * @param startSequence 消息序列号，标记该序列号及之前的消息为已读
 * @param startTime 消息的 Unix 时间戳（秒）
 */
suspend fun AbstractBot.markFriendMessagesAsRead(
    friendUin: Long,
    startSequence: Long,
    startTime: Long
) = client.callService(
    ReportMessageRead,
    ReportMessageRead.Req(
        groupUin = null,
        targetUid = getUidByUin(friendUin),
        startSequence = startSequence,
        time = startTime
    )
)

/**
 * 标记群消息为已读
 * @param groupUin 群号
 * @param startSequence 消息序列号，标记该序列号及之前的消息为已读
 */
suspend fun AbstractBot.markGroupMessagesAsRead(
    groupUin: Long,
    startSequence: Long
) = client.callService(
    ReportMessageRead,
    ReportMessageRead.Req(
        groupUin = groupUin,
        targetUid = null,
        startSequence = startSequence,
        time = 0L
    )
)
