package org.ntqqrev.acidify.message.internal

import dev.karmakrafts.kompress.Deflater
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.getFriendHistoryMessages
import org.ntqqrev.acidify.getGroupHistoryMessages
import org.ntqqrev.acidify.getUidByUin
import org.ntqqrev.acidify.internal.crypto.hash.MD5
import org.ntqqrev.acidify.internal.json.message.OutgoingForwardPayload
import org.ntqqrev.acidify.internal.proto.message.*
import org.ntqqrev.acidify.internal.proto.message.elem.*
import org.ntqqrev.acidify.internal.proto.message.extra.QBigFaceExtra
import org.ntqqrev.acidify.internal.proto.message.extra.QSmallFaceExtra
import org.ntqqrev.acidify.internal.proto.message.extra.SourceMsgResvAttr
import org.ntqqrev.acidify.internal.proto.message.extra.TextResvAttr
import org.ntqqrev.acidify.internal.service.message.RichMediaUpload
import org.ntqqrev.acidify.internal.service.message.SendLongMsg
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.sha1
import org.ntqqrev.acidify.message.BotOutgoingSegment
import org.ntqqrev.acidify.message.MessageScene
import kotlin.random.Random
import kotlin.time.Clock

internal class MessageBuildingContext(
    val bot: Bot,
    val scene: MessageScene,
    val peerUin: Long,
    val peerUid: String,
    val segments: List<BotOutgoingSegment>,
    private val nestedForwardTrace: MutableMap<String, List<CommonMessage>>? = null
) {
    private val logger = bot.createLogger(this)
    private val elemsList = mutableListOf<Deferred<List<Elem>>>()

    private fun addAsync(elem: suspend () -> Elem) {
        elemsList.add(bot.async { listOf(elem()) })
    }

    private fun addMultipleAsync(elems: suspend () -> List<Elem>) {
        elemsList.add(bot.async { elems() })
    }

    fun BotOutgoingSegment.Text.build() = addAsync {
        Elem(text = Text(textMsg = text))
    }

    fun BotOutgoingSegment.Mention.build() = addAsync {
        Elem(
            text = Text(
                textMsg = "@$name",
                pbReserve = TextResvAttr(
                    atType = if (uin == null) 1 else 2,
                    atMemberUin = uin ?: 0L,
                    atMemberUid = if (uin != null) bot.getUidByUin(uin) else "",
                ).pbEncode()
            )
        )
    }

    fun BotOutgoingSegment.Face.build() = addAsync {
        val faceDetail = bot.faceDetailMap[faceId.toString()]
            ?: throw NoSuchElementException("要发送的表情 ID 不存在: $faceId")

        if (isLarge) {
            Elem(
                commonElem = CommonElem(
                    serviceType = 37,
                    pbElem = QBigFaceExtra(
                        aniStickerPackId = faceDetail.aniStickerPackId.toString(),
                        aniStickerId = faceDetail.aniStickerId.toString(),
                        faceId = faceId,
                        field4 = 1,
                        aniStickerType = faceDetail.aniStickerType,
                        field6 = "",
                        preview = faceDetail.qDes,
                        field9 = 1,
                    ).pbEncode(),
                    businessType = faceDetail.aniStickerType,
                )
            )
        }

        if (faceId >= 260) {
            Elem(
                commonElem = CommonElem(
                    serviceType = 33,
                    pbElem = QSmallFaceExtra(
                        faceId = faceId,
                        text = faceDetail.qDes,
                        compatText = faceDetail.qDes,
                    ).pbEncode(),
                    businessType = faceDetail.aniStickerType,
                )
            )
        }

        Elem(face = Face(index = faceId))
    }

    fun BotOutgoingSegment.Reply.build() = addMultipleAsync {
        val replied = when (scene) {
            MessageScene.FRIEND -> bot::getFriendHistoryMessages
            MessageScene.GROUP -> bot::getGroupHistoryMessages
            else -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }(peerUin, 1, sequence).messages.firstOrNull()

        if (replied == null) {
            logger.w { "无法引用消息: 找不到对应的消息 (seq=$sequence)" }
            return@addMultipleAsync emptyList()
        }

        val srcMsgElem = Elem(
            srcMsg = SourceMsg(
                origSeqs = listOf(
                    if (scene == MessageScene.FRIEND) replied.clientSequence
                    else replied.sequence
                ),
                senderUin = replied.senderUin,
                time = replied.timestamp,
                flag = 0,
                elems = replied.raw.messageBody.richText.elems,
                pbReserve = SourceMsgResvAttr(
                    oriMsgType = 2,
                    sourceMsgId = replied.messageUid,
                    senderUid = replied.senderUid,
                ).pbEncode(),
            )
        )

        // 群消息需要额外添加 @ 提及（客户端会根据 uid 显示昵称）
        if (scene == MessageScene.GROUP) {
            listOf(
                srcMsgElem,
                Elem(
                    text = Text(
                        textMsg = "@${replied.senderUin}",
                        pbReserve = TextResvAttr(
                            atType = 2,
                            atMemberUin = replied.senderUin,
                            atMemberUid = replied.senderUid,
                        ).pbEncode()
                    )
                ),
                Elem(text = Text(textMsg = " "))
            )
        } else {
            listOf(srcMsgElem)
        }
    }

    fun BotOutgoingSegment.Image.build() = addMultipleAsync {
        val imageMd5Bytes = MD5.hash(raw)
        val imageMd5 = imageMd5Bytes.toHexString()
        val imageSha1Bytes = raw.sha1()
        val imageSha1 = imageSha1Bytes.toHexString()

        val uploadResp = when (scene) {
            MessageScene.FRIEND -> {
                bot.client.callService(
                    RichMediaUpload.PrivateImage,
                    RichMediaUpload.ImageUploadRequest(
                        imageData = raw,
                        imageMd5 = imageMd5,
                        imageSha1 = imageSha1,
                        imageExt = ".${format.ext}",
                        width = width,
                        height = height,
                        picFormat = format.underlying,
                        subType = subType.underlying,
                        textSummary = summary
                    )
                )
            }

            MessageScene.GROUP -> {
                bot.client.callService(
                    RichMediaUpload.GroupImage,
                    RichMediaUpload.ImageUploadRequest(
                        imageData = raw,
                        imageMd5 = imageMd5,
                        imageSha1 = imageSha1,
                        imageExt = ".${format.ext}",
                        width = width,
                        height = height,
                        picFormat = format.underlying,
                        subType = subType.underlying,
                        textSummary = summary,
                        groupUin = peerUin
                    )
                )
            }

            else -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        if (uploadResp.uKey.isNotEmpty()) {
            bot.client.highwayContext.uploadImage(
                image = raw,
                imageMd5 = imageMd5Bytes,
                imageSha1 = imageSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } else {
            logger.d { "uKey 为空，服务器可能已存在该图片，跳过上传" }
        }

        val msgInfoBuf = uploadResp.msgInfoBuf
        val businessType = when (scene) {
            MessageScene.FRIEND -> 10
            MessageScene.GROUP -> 20
            MessageScene.TEMP -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        buildList {
            when (scene) {
                MessageScene.FRIEND -> add(
                    Elem(
                        notOnlineImage = uploadResp.compatQMsg
                    )
                )

                MessageScene.GROUP -> add(
                    Elem(
                        customFace = uploadResp.compatQMsg
                    )
                )

                MessageScene.TEMP -> {}
            }

            add(
                Elem(
                    commonElem = CommonElem(
                        serviceType = 48,
                        pbElem = msgInfoBuf,
                        businessType = businessType,
                    )
                )
            )
        }
    }

    fun BotOutgoingSegment.Record.build() = addAsync {
        val recordMd5Bytes = MD5.hash(rawSilk)
        val recordMd5 = recordMd5Bytes.toHexString()
        val recordSha1Bytes = rawSilk.sha1()
        val recordSha1 = recordSha1Bytes.toHexString()

        val uploadResp = when (scene) {
            MessageScene.FRIEND -> {
                bot.client.callService(
                    RichMediaUpload.PrivateRecord,
                    RichMediaUpload.RecordUploadRequest(
                        audioData = rawSilk,
                        audioMd5 = recordMd5,
                        audioSha1 = recordSha1,
                        audioDuration = duration.toInt()
                    )
                )
            }

            MessageScene.GROUP -> {
                bot.client.callService(
                    RichMediaUpload.GroupRecord,
                    RichMediaUpload.RecordUploadRequest(
                        audioData = rawSilk,
                        audioMd5 = recordMd5,
                        audioSha1 = recordSha1,
                        audioDuration = duration.toInt(),
                        groupUin = peerUin
                    )
                )
            }

            else -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        if (uploadResp.uKey.isNotEmpty()) {
            bot.client.highwayContext.uploadRecord(
                record = rawSilk,
                recordMd5 = recordMd5Bytes,
                recordSha1 = recordSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } else {
            logger.d { "uKey 为空，服务器可能已存在该语音，跳过上传" }
        }

        val msgInfoBuf = uploadResp.msgInfoBuf
        val businessType = when (scene) {
            MessageScene.FRIEND -> 12
            MessageScene.GROUP -> 22
            MessageScene.TEMP -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        Elem(
            commonElem = CommonElem(
                serviceType = 48,
                pbElem = msgInfoBuf,
                businessType = businessType,
            )
        )
    }

    fun BotOutgoingSegment.Video.build() = addAsync {
        val videoMd5Bytes = MD5.hash(raw)
        val videoMd5 = videoMd5Bytes.toHexString()
        val videoSha1Bytes = raw.sha1()
        val videoSha1 = videoSha1Bytes.toHexString()

        val thumbMd5Bytes = MD5.hash(thumb)
        val thumbMd5 = thumbMd5Bytes.toHexString()
        val thumbSha1Bytes = thumb.sha1()
        val thumbSha1 = thumbSha1Bytes.toHexString()

        val uploadResp = when (scene) {
            MessageScene.FRIEND -> {
                bot.client.callService(
                    RichMediaUpload.PrivateVideo,
                    RichMediaUpload.VideoUploadRequest(
                        videoData = raw,
                        videoMd5 = videoMd5,
                        videoSha1 = videoSha1,
                        videoWidth = width,
                        videoHeight = height,
                        videoDuration = duration.toInt(),
                        thumbnailData = thumb,
                        thumbnailMd5 = thumbMd5,
                        thumbnailSha1 = thumbSha1,
                        thumbnailExt = thumbFormat.ext,
                        thumbnailPicFormat = thumbFormat.underlying
                    )
                )
            }

            MessageScene.GROUP -> {
                bot.client.callService(
                    RichMediaUpload.GroupVideo,
                    RichMediaUpload.VideoUploadRequest(
                        videoData = raw,
                        videoMd5 = videoMd5,
                        videoSha1 = videoSha1,
                        videoWidth = width,
                        videoHeight = height,
                        videoDuration = duration.toInt(),
                        thumbnailData = thumb,
                        thumbnailMd5 = thumbMd5,
                        thumbnailSha1 = thumbSha1,
                        thumbnailExt = thumbFormat.ext,
                        thumbnailPicFormat = thumbFormat.underlying,
                        groupUin = peerUin
                    )
                )
            }

            else -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        if (uploadResp.uKey.isNotEmpty()) {
            // TODO: fix highway here
            bot.client.flashTransferContext.uploadFile(
                uKey = uploadResp.uKey,
                appId = if (scene == MessageScene.FRIEND) 1413 else 1415,
                bodyStream = raw,
            )
        } else {
            logger.d { "uKey 为空，服务器可能已存在该视频，跳过上传" }
        }

        if (uploadResp.subFileInfos.firstOrNull()?.uKey?.isNotEmpty() == true) {
            bot.client.highwayContext.uploadVideoThumbnail(
                thumbnail = thumb,
                thumbnailMd5 = thumbMd5Bytes,
                thumbnailSha1 = thumbSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } else {
            logger.d { "视频缩略图 uKey 为空，服务器可能已存在该缩略图，跳过上传" }
        }

        val msgInfoBuf = uploadResp.msgInfoBuf
        val businessType = when (scene) {
            MessageScene.FRIEND -> 11
            MessageScene.GROUP -> 21
            MessageScene.TEMP -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        Elem(
            commonElem = CommonElem(
                serviceType = 48,
                pbElem = msgInfoBuf,
                businessType = businessType,
            )
        )
    }

    fun BotOutgoingSegment.Forward.build() = addAsync {
        val outerThis = this@MessageBuildingContext
        val forwardCtx = Forward(outerThis, nodes)
        val commonMessages = forwardCtx.build()
        val uuid = Random.nextBytes(16).let {
            "${it.sliceArray(0..3).toHexString()}-${it.sliceArray(4..5).toHexString()}-" +
                    "${it.sliceArray(6..7).toHexString()}-${it.sliceArray(8..9).toHexString()}-" +
                    it.sliceArray(10..15).toHexString()
        }
        nestedForwardTrace?.set(uuid, commonMessages)

        val resId = bot.client.callService(
            SendLongMsg,
            SendLongMsg.Req(
                scene = scene,
                peerUin = peerUin,
                peerUid = peerUid,
                messages = commonMessages,
                nestedForwardTrace = if (nestedForwardTrace == null)
                    forwardCtx.nestedForwardTrace
                else mutableMapOf()
            )
        )

        val lightApp = OutgoingForwardPayload(
            config = buildJsonObject {
                put("autosize", 1)
                put("forward", 1)
                put("round", 1)
                put("type", "normal")
                put("width", 300)
            },
            meta = buildJsonObject {
                put("detail", buildJsonObject {
                    put("news", buildJsonArray {
                        preview.forEach {
                            add(buildJsonObject {
                                put("text", it)
                            })
                        }
                    })
                    put("resid", resId)
                    put("source", title)
                    put("summary", summary)
                    put("uniseq", uuid)
                })
            },
            desc = "[聊天记录]",
            extra = Json.encodeToString(buildJsonObject {
                put("filename", uuid)
                put("tsum", nodes.size)
            }),
            prompt = prompt,
            ver = "0.0.0.5",
            view = "contact"
        )
        val str = Json.encodeToString(lightApp)
        val buffer = Buffer()
        buffer.writeByte(0x01)
        buffer.write(Deflater.deflate(str.encodeToByteArray(), raw = false))

        Elem(
            lightAppElem = LightAppElem(
                bytesData = buffer.readByteArray()
            )
        )
    }

    suspend fun build(): List<Elem> {
        segments.forEach {
            when (it) {
                is BotOutgoingSegment.Text -> it.build()
                is BotOutgoingSegment.Mention -> it.build()
                is BotOutgoingSegment.Face -> it.build()
                is BotOutgoingSegment.Reply -> it.build()
                is BotOutgoingSegment.Image -> it.build()
                is BotOutgoingSegment.Record -> it.build()
                is BotOutgoingSegment.Video -> it.build()
                is BotOutgoingSegment.Forward -> it.build()
            }
        }
        return elemsList.awaitAll().flatten()
    }

    internal class Forward(
        val ctx: MessageBuildingContext,
        val nodes: List<BotOutgoingSegment.Forward.Node>,
    ) {
        private val commonMsgList = mutableListOf<Deferred<CommonMessage>>()
        val nestedForwardTrace = mutableMapOf<String, List<CommonMessage>>()

        private fun addAsync(elem: suspend () -> CommonMessage) {
            commonMsgList.add(ctx.bot.async { elem() })
        }

        fun BotOutgoingSegment.Forward.Node.build() = addAsync {
            val subCtx = MessageBuildingContext(
                bot = ctx.bot,
                scene = ctx.scene,
                peerUin = ctx.peerUin,
                peerUid = ctx.peerUid,
                segments = segments,
                nestedForwardTrace = ctx.nestedForwardTrace ?: nestedForwardTrace,
            )
            val subElems = subCtx.build()
            val fakeSequence = Random.nextInt(1000000, 9999999).toLong()
            CommonMessage(
                routingHead = RoutingHead(
                    fromUin = senderUin,
                    toUid = ctx.bot.uid,
                    commonC2C = if (ctx.scene == MessageScene.FRIEND) {
                        RoutingHead.CommonC2C(name = senderName)
                    } else {
                        RoutingHead.CommonC2C()
                    },
                    group = if (ctx.scene == MessageScene.GROUP) {
                        RoutingHead.CommonGroup(
                            groupCode = ctx.peerUin,
                            groupCard = senderName,
                            groupCardType = 2,
                        )
                    } else {
                        RoutingHead.CommonGroup()
                    }
                ),
                contentHead = ContentHead(
                    type = when (ctx.scene) {
                        MessageScene.FRIEND -> PushMsgType.FriendMessage.value
                        MessageScene.GROUP -> PushMsgType.GroupMessage.value
                        MessageScene.TEMP -> PushMsgType.TempMessage.value
                    },
                    random = Random.nextInt(),
                    sequence = fakeSequence,
                    time = Clock.System.now().epochSeconds,
                    clientSequence = fakeSequence,
                    msgUid = Random.nextLong(1000000000000, 9999999999999),
                    forwardExt = ContentHead.Forward(
                        field3 = 2,
                        avatar = "https://q.qlogo.cn/headimg_dl?dst_uin=$senderUin&spec=640&img_type=jpg"
                    ),
                ),
                messageBody = MessageBody(
                    richText = RichText(elems = subElems)
                ),
            )
        }

        suspend fun build(): List<CommonMessage> {
            nodes.forEach { it.build() }
            return commonMsgList.awaitAll()
        }
    }
}
