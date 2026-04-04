package org.ntqqrev.acidify.milky.api.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.message.MessageScene
import org.ntqqrev.acidify.milky.api.MilkyApiException
import org.ntqqrev.acidify.milky.api.define
import org.ntqqrev.acidify.milky.transform.toMessageScene
import org.ntqqrev.acidify.milky.transform.transformForwardedMessage
import org.ntqqrev.acidify.milky.transform.transformIncomingMessage
import org.ntqqrev.acidify.milky.transform.transformOutgoingSegment
import org.ntqqrev.milky.*

val SendPrivateMessage = ApiEndpoint.SendPrivateMessage.define {
    bot.getFriend(it.userId)
        ?: throw MilkyApiException(-404, "Friend not found")
    val result = bot.sendFriendMessage(
        friendUin = it.userId,
        segments = it.message.map { segment ->
            with(application) {
                async {
                    transformOutgoingSegment(
                        scene = MessageScene.FRIEND,
                        peerUin = it.userId,
                        segment = segment
                    )
                }
            }
        }.awaitAll()
    )
    SendPrivateMessageOutput(
        messageSeq = result.sequence,
        time = result.sendTime
    )
}

val SendGroupMessage = ApiEndpoint.SendGroupMessage.define {
    bot.getGroup(it.groupId)
        ?: throw MilkyApiException(-404, "Group not found")
    val result = bot.sendGroupMessage(
        groupUin = it.groupId,
        segments = it.message.map { segment ->
            with(application) {
                async {
                    transformOutgoingSegment(
                        scene = MessageScene.GROUP,
                        peerUin = it.groupId,
                        segment = segment
                    )
                }
            }
        }.awaitAll()
    )
    SendGroupMessageOutput(
        messageSeq = result.sequence,
        time = result.sendTime
    )
}

val RecallPrivateMessage = ApiEndpoint.RecallPrivateMessage.define {
    bot.getFriend(it.userId)
        ?: throw MilkyApiException(-404, "Friend not found")
    bot.recallFriendMessage(
        friendUin = it.userId,
        sequence = it.messageSeq
    )
    RecallPrivateMessageOutput()
}

val RecallGroupMessage = ApiEndpoint.RecallGroupMessage.define {
    bot.getGroup(it.groupId)
        ?: throw MilkyApiException(-404, "Group not found")
    bot.recallGroupMessage(
        groupUin = it.groupId,
        sequence = it.messageSeq
    )
    RecallGroupMessageOutput()
}

val GetMessage = ApiEndpoint.GetMessage.define {
    val messages = when (it.messageScene.toMessageScene()) {
        MessageScene.FRIEND -> {
            bot.getFriend(it.peerId)
                ?: throw MilkyApiException(-404, "Friend not found")
            bot.getFriendHistoryMessages(
                friendUin = it.peerId,
                limit = 1,
                startSequence = it.messageSeq
            )
        }

        MessageScene.GROUP -> {
            bot.getGroup(it.peerId)
                ?: throw MilkyApiException(-404, "Group not found")
            bot.getGroupHistoryMessages(
                groupUin = it.peerId,
                limit = 1,
                startSequence = it.messageSeq
            )
        }

        else -> throw MilkyApiException(-400, "Unsupported message scene")
    }
    val message = messages.messages.firstOrNull()
        ?: throw MilkyApiException(-404, "Message not found")
    val transformedMessage = transformIncomingMessage(message)
        ?: throw MilkyApiException(-404, "Message transformation failed")
    GetMessageOutput(
        message = transformedMessage
    )
}

val GetHistoryMessages = ApiEndpoint.GetHistoryMessages.define {
    if (it.limit !in 1..30) {
        throw MilkyApiException(-400, "Limit must be between 1 and 30")
    }
    val historyMessages = when (it.messageScene.toMessageScene()) {
        MessageScene.FRIEND -> {
            bot.getFriend(it.peerId) ?: throw MilkyApiException(-404, "Friend not found")
            bot.getFriendHistoryMessages(
                friendUin = it.peerId,
                limit = it.limit,
                startSequence = it.startMessageSeq
            )
        }

        MessageScene.GROUP -> {
            bot.getGroup(it.peerId) ?: throw MilkyApiException(-404, "Group not found")
            bot.getGroupHistoryMessages(
                groupUin = it.peerId,
                limit = it.limit,
                startSequence = it.startMessageSeq
            )
        }

        else -> throw MilkyApiException(-400, "Unsupported message scene")
    }
    val transformedMessages = historyMessages.messages.mapNotNull { msg ->
        transformIncomingMessage(msg)
    }
    GetHistoryMessagesOutput(
        messages = transformedMessages,
        nextMessageSeq = historyMessages.nextStartSequence
    )
}

val GetResourceTempUrl = ApiEndpoint.GetResourceTempUrl.define {
    GetResourceTempUrlOutput(
        url = bot.getDownloadUrl(it.resourceId)
    )
}

val GetForwardedMessages = ApiEndpoint.GetForwardedMessages.define {
    val forwardedMessages = bot.getForwardedMessages(it.forwardId)
    val transformedMessages = forwardedMessages.map { msg ->
        with(application) {
            async { transformForwardedMessage(msg) }
        }
    }.awaitAll()
    GetForwardedMessagesOutput(
        messages = transformedMessages
    )
}

val MarkMessageAsRead = ApiEndpoint.MarkMessageAsRead.define {
    when (it.messageScene.toMessageScene()) {
        MessageScene.FRIEND -> {
            bot.getFriend(it.peerId) ?: throw MilkyApiException(-404, "Friend not found")
            // Get the message time from history
            val messages = bot.getFriendHistoryMessages(
                friendUin = it.peerId,
                limit = 1,
                startSequence = it.messageSeq
            )
            val message = messages.messages.firstOrNull()
                ?: throw MilkyApiException(-404, "Message not found")
            bot.markFriendMessagesAsRead(
                friendUin = it.peerId,
                startSequence = it.messageSeq,
                startTime = message.timestamp
            )
        }

        MessageScene.GROUP -> {
            bot.getGroup(it.peerId) ?: throw MilkyApiException(-404, "Group not found")
            bot.markGroupMessagesAsRead(
                groupUin = it.peerId,
                startSequence = it.messageSeq
            )
        }

        else -> throw MilkyApiException(-400, "Unsupported message scene")
    }
    MarkMessageAsReadOutput()
}