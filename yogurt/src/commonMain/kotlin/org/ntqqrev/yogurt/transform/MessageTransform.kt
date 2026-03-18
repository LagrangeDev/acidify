package org.ntqqrev.yogurt.transform

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.codec.ImageInfo
import org.ntqqrev.acidify.codec.calculatePcmDuration
import org.ntqqrev.acidify.codec.getImageInfo
import org.ntqqrev.acidify.getDownloadUrl
import org.ntqqrev.acidify.getFriend
import org.ntqqrev.acidify.getGroup
import org.ntqqrev.acidify.message.*
import org.ntqqrev.milky.*
import org.ntqqrev.yogurt.util.FFMpegCodec
import org.ntqqrev.yogurt.util.resolveUri

suspend fun Application.transformMessage(msg: BotIncomingMessage): IncomingMessage? {
    val bot = dependencies.resolve<AbstractBot>()
    return when (msg.scene) {
        MessageScene.FRIEND -> {
            val friend = bot.getFriend(msg.peerUin) ?: return null
            IncomingMessage.Friend(
                peerId = msg.peerUin,
                messageSeq = msg.sequence,
                senderId = msg.senderUin,
                time = msg.timestamp,
                segments = msg.segments.map {
                    async { transformSegment(it) }
                }.awaitAll(),
                friend = friend.toMilkyEntity()
            )
        }

        MessageScene.GROUP -> {
            val group = bot.getGroup(msg.peerUin) ?: return null
            val member = group.getMember(msg.senderUin) ?: return null
            IncomingMessage.Group(
                peerId = msg.peerUin,
                messageSeq = msg.sequence,
                senderId = msg.senderUin,
                time = msg.timestamp,
                segments = msg.segments.map {
                    async { transformSegment(it) }
                }.awaitAll(),
                group = group.toMilkyEntity(),
                groupMember = member.toMilkyEntity(),
            )
        }

        else -> null
    }
}

suspend fun Application.transformForwardedMessage(msg: BotForwardedMessage): IncomingForwardedMessage {
    return IncomingForwardedMessage(
        messageSeq = msg.sequence,
        senderName = msg.senderName,
        avatarUrl = msg.avatarUrl,
        time = msg.timestamp,
        segments = msg.segments.map { segment ->
            async { transformSegment(segment) }
        }.awaitAll()
    )
}

suspend fun Application.transformSegment(segment: BotIncomingSegment): IncomingSegment {
    val bot = dependencies.resolve<AbstractBot>()
    return when (segment) {
        is BotIncomingSegment.Text -> IncomingSegment.Text(
            data = IncomingSegment.Text.Data(
                text = segment.text
            )
        )

        is BotIncomingSegment.Mention -> if (segment.uin != null) {
            IncomingSegment.Mention(
                data = IncomingSegment.Mention.Data(
                    userId = segment.uin!!,
                    name = segment.name,
                )
            )
        } else {
            IncomingSegment.MentionAll(
                data = IncomingSegment.MentionAll.Data()
            )
        }

        is BotIncomingSegment.Face -> IncomingSegment.Face(
            data = IncomingSegment.Face.Data(
                faceId = segment.faceId.toString(),
                isLarge = segment.isLarge,
            )
        )

        is BotIncomingSegment.Reply -> IncomingSegment.Reply(
            data = IncomingSegment.Reply.Data(
                messageSeq = segment.sequence,
                senderId = segment.senderUin,
                senderName = segment.senderName,
                time = segment.timestamp,
                segments = segment.segments.map {
                    async { transformSegment(it) }
                }.awaitAll(),
            )
        )

        is BotIncomingSegment.Image -> IncomingSegment.Image(
            data = IncomingSegment.Image.Data(
                resourceId = segment.fileId,
                tempUrl = bot.getDownloadUrl(segment.fileId),
                width = segment.width,
                height = segment.height,
                subType = segment.subType.toMilkyString(),
                summary = segment.summary,
            )
        )

        is BotIncomingSegment.Record -> IncomingSegment.Record(
            data = IncomingSegment.Record.Data(
                resourceId = segment.fileId,
                tempUrl = bot.getDownloadUrl(segment.fileId),
                duration = segment.duration
            )
        )

        is BotIncomingSegment.Video -> IncomingSegment.Video(
            data = IncomingSegment.Video.Data(
                resourceId = segment.fileId,
                tempUrl = bot.getDownloadUrl(segment.fileId),
                duration = segment.duration,
                width = segment.width,
                height = segment.height
            )
        )

        is BotIncomingSegment.File -> IncomingSegment.File(
            data = IncomingSegment.File.Data(
                fileId = segment.fileId,
                fileName = segment.fileName,
                fileSize = segment.fileSize,
                fileHash = segment.fileHash
            )
        )

        is BotIncomingSegment.Forward -> IncomingSegment.Forward(
            data = IncomingSegment.Forward.Data(
                forwardId = segment.resId,
                title = segment.title,
                preview = segment.preview,
                summary = segment.summary,
            )
        )

        is BotIncomingSegment.MarketFace -> IncomingSegment.MarketFace(
            data = IncomingSegment.MarketFace.Data(
                emojiPackageId = segment.emojiPackageId,
                emojiId = segment.emojiId,
                key = segment.key,
                summary = segment.summary,
                url = segment.url,
            )
        )

        is BotIncomingSegment.LightApp -> IncomingSegment.LightApp(
            data = IncomingSegment.LightApp.Data(
                appName = segment.appName,
                jsonPayload = segment.jsonPayload
            )
        )
    }
}

suspend fun Application.transformSegment(
    scene: MessageScene,
    peerUin: Long,
    segment: OutgoingSegment,
): BotOutgoingSegment {
    val bot = dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("MessageTransform")
    return when (segment) {
        is OutgoingSegment.Text -> BotOutgoingSegment.Text(
            text = segment.data.text
        )

        is OutgoingSegment.Mention -> {
            if (scene == MessageScene.FRIEND) {
                // 私聊不支持 at，转换为文本
                BotOutgoingSegment.Text("@${segment.data.userId} ")
            }
            val group = bot.getGroup(peerUin)
            val member = group?.getMember(segment.data.userId)
            BotOutgoingSegment.Mention(
                uin = segment.data.userId,
                name = member?.card?.takeIf { it.isNotEmpty() }
                    ?: member?.nickname
                    ?: segment.data.userId.toString(),
            )
        }

        is OutgoingSegment.MentionAll -> if (scene == MessageScene.FRIEND) {
            // 私聊不支持 at，转换为文本
            BotOutgoingSegment.Text("@全体成员 ")
        } else BotOutgoingSegment.Mention(
            uin = null,
            name = "全体成员",
        )

        is OutgoingSegment.Face -> BotOutgoingSegment.Face(
            faceId = segment.data.faceId.toInt(),
            isLarge = false,
        )

        is OutgoingSegment.Reply -> BotOutgoingSegment.Reply(
            sequence = segment.data.messageSeq,
        )

        is OutgoingSegment.Image -> {
            val imageData = resolveUri(segment.data.uri)
            val imageInfo = getImageInfo(imageData)
            BotOutgoingSegment.Image(
                raw = imageData,
                format = imageInfo.format.toAcidifyFormat(),
                width = imageInfo.width,
                height = imageInfo.height,
                subType = segment.data.subType.toImageSubType(),
                summary = segment.data.summary ?: "[图片]"
            )
        }

        is OutgoingSegment.Record -> {
            val audioData = resolveUri(segment.data.uri)
            // 尝试转换为 PCM，若失败则假设已是 PCM 格式
            val pcmData = try {
                FFMpegCodec.audioToPcm(audioData)
            } catch (e: Exception) {
                logger.w(e) { "语音 ${segment.data.uri} 转 PCM 失败，尝试直接编码" }
                audioData
            }
            val silkData = FFMpegCodec.silkEncode(pcmData)
            val duration = calculatePcmDuration(pcmData)
            logger.d { "语音 ${segment.data.uri} 编码完成，时长 ${duration.inWholeSeconds} 秒" }
            BotOutgoingSegment.Record(
                rawSilk = silkData,
                duration = duration.inWholeSeconds
            )
        }

        is OutgoingSegment.Video -> {
            val videoData = resolveUri(segment.data.uri)
            val videoInfo = FFMpegCodec.getVideoInfo(videoData)
            logger.d { "视频 ${segment.data.uri} 信息：${videoInfo.width}x${videoInfo.height}，时长 ${videoInfo.duration.inWholeSeconds} 秒" }
            val thumbData = if (segment.data.thumbUri != null) {
                resolveUri(segment.data.thumbUri!!)
            } else {
                FFMpegCodec.getVideoFirstFrameJpg(videoData)
            }
            val thumbInfo = getImageInfo(thumbData)
            BotOutgoingSegment.Video(
                raw = videoData,
                width = videoInfo.width,
                height = videoInfo.height,
                duration = videoInfo.duration.inWholeSeconds,
                thumb = thumbData,
                thumbFormat = thumbInfo.format.toAcidifyFormat()
            )
        }

        is OutgoingSegment.Forward -> {
            val nodes = segment.data.messages.map { msg ->
                BotOutgoingSegment.Forward.Node(
                    senderUin = msg.userId,
                    senderName = msg.senderName,
                    segments = msg.segments.map { seg ->
                        async { transformSegment(scene, peerUin, seg) }
                    }.awaitAll()
                )
            }
            BotOutgoingSegment.Forward(
                nodes = nodes,
                title = segment.data.title ?: "群聊的聊天记录",
                preview = segment.data.preview ?: nodes.take(4).map {
                    it.senderName + ": " + it.segments.joinToString("")
                },
                summary = segment.data.summary ?: "查看${segment.data.messages.size}条转发消息",
                prompt = segment.data.prompt ?: "[聊天记录]"
            )
        }

        is OutgoingSegment.LightApp -> BotOutgoingSegment.LightApp(
            jsonPayload = segment.data.jsonPayload
        )
    }
}

suspend fun Application.transformEssenceMessage(msg: BotEssenceMessage): GroupEssenceMessage {
    return GroupEssenceMessage(
        groupId = msg.groupUin,
        messageSeq = msg.messageSeq,
        messageTime = msg.messageTime,
        senderId = msg.senderUin,
        senderName = msg.senderName,
        operatorId = msg.operatorUin,
        operatorName = msg.operatorName,
        operationTime = msg.operationTime,
        segments = msg.segments.map { segment ->
            async { transformEssenceSegment(segment) }
        }.awaitAll()
    )
}

suspend fun Application.transformEssenceSegment(segment: BotEssenceSegment): IncomingSegment {
    val bot = dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("MessageTransform")
    return when (segment) {
        is BotEssenceSegment.Text -> IncomingSegment.Text(
            data = IncomingSegment.Text.Data(
                text = segment.text
            )
        )

        is BotEssenceSegment.Face -> IncomingSegment.Face(
            data = IncomingSegment.Face.Data(
                faceId = segment.faceId.toString(),
                isLarge = false,
            )
        )

        is BotEssenceSegment.Image -> {
            val imageData = resolveUri(segment.imageUrl)
            val imageInfo = try {
                getImageInfo(imageData)
            } catch (e: Exception) {
                logger.w(e) { "解析精华消息图像信息失败，使用缺省值" }
                ImageInfo(
                    format = org.ntqqrev.acidify.codec.ImageFormat.PNG,
                    width = 300,
                    height = 300
                )
            }
            IncomingSegment.Image(
                data = IncomingSegment.Image.Data(
                    resourceId = segment.imageUrl,
                    tempUrl = segment.imageUrl,
                    width = imageInfo.width,
                    height = imageInfo.height,
                    summary = "[图片]",
                    subType = "normal"
                )
            )
        }

        is BotEssenceSegment.Video -> {
            // also transform to image
            val imageData = resolveUri(segment.thumbnailUrl)
            val imageInfo = getImageInfo(imageData)
            IncomingSegment.Image(
                data = IncomingSegment.Image.Data(
                    resourceId = segment.thumbnailUrl,
                    tempUrl = segment.thumbnailUrl,
                    width = imageInfo.width,
                    height = imageInfo.height,
                    summary = "[图片]",
                    subType = "normal"
                )
            )
        }
    }
}

fun ImageSubType.toMilkyString() = when (this) {
    ImageSubType.NORMAL -> "normal"
    ImageSubType.STICKER -> "sticker"
}

fun String.toImageSubType() = when (this) {
    "normal" -> ImageSubType.NORMAL
    "sticker" -> ImageSubType.STICKER
    else -> ImageSubType.NORMAL
}

fun String.toMessageScene() = when (this) {
    "friend" -> MessageScene.FRIEND
    "group" -> MessageScene.GROUP
    "temp" -> MessageScene.TEMP
    else -> throw IllegalArgumentException("Unknown message scene: $this")
}

fun org.ntqqrev.acidify.codec.ImageFormat.toAcidifyFormat() = when (this) {
    org.ntqqrev.acidify.codec.ImageFormat.PNG -> ImageFormat.PNG
    org.ntqqrev.acidify.codec.ImageFormat.GIF -> ImageFormat.GIF
    org.ntqqrev.acidify.codec.ImageFormat.JPEG -> ImageFormat.JPEG
    org.ntqqrev.acidify.codec.ImageFormat.BMP -> ImageFormat.BMP
    org.ntqqrev.acidify.codec.ImageFormat.WEBP -> ImageFormat.WEBP
    org.ntqqrev.acidify.codec.ImageFormat.TIFF -> ImageFormat.TIFF
}