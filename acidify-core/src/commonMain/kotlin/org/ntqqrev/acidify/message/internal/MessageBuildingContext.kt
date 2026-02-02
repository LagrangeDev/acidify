package org.ntqqrev.acidify.message.internal

import korlibs.io.compression.compress
import korlibs.io.compression.deflate.ZLib
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
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.internal.util.sha1
import org.ntqqrev.acidify.message.*
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class MessageBuildingContext(
    val bot: Bot,
    val scene: MessageScene,
    val peerUin: Long,
    val peerUid: String,
    private val nestedForwardTrace: MutableMap<String, List<CommonMessage>>? = null
) : BotOutgoingMessageBuilder {
    private val logger = bot.createLogger(this)
    private val elemsList = mutableListOf<Deferred<List<Elem>>>()

    private fun addAsync(elem: suspend () -> Elem) {
        elemsList.add(bot.async { listOf(elem()) })
    }

    private fun addMultipleAsync(elems: suspend () -> List<Elem>) {
        elemsList.add(bot.async { elems() })
    }

    override fun text(text: String) = addAsync {
        Elem(text = Text(textMsg = text))
    }

    override fun mention(uin: Long?, name: String) = addAsync {
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

    override fun face(faceId: Int, isLarge: Boolean) = addAsync {
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

    override fun reply(sequence: Long) = addMultipleAsync {
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

    override fun image(
        raw: ByteArray,
        format: ImageFormat,
        width: Int,
        height: Int,
        subType: ImageSubType,
        summary: String
    ) = addMultipleAsync {
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

        uploadResp.uKey.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadImage(
                image = raw,
                imageMd5 = imageMd5Bytes,
                imageSha1 = imageSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "uKey 为空，服务器可能已存在该图片，跳过上传" }

        val msgInfo = uploadResp.msgInfo
        val businessType = when (scene) {
            MessageScene.FRIEND -> 10
            MessageScene.GROUP -> 20
            MessageScene.TEMP -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        listOf(
            when (scene) {
                MessageScene.FRIEND -> Elem(
                    notOnlineImage = uploadResp.compatQMsg.pbDecode<NotOnlineImage>()
                )

                MessageScene.GROUP -> Elem(
                    customFace = uploadResp.compatQMsg.pbDecode<CustomFace>()
                )

                MessageScene.TEMP -> Elem()
            },
            Elem(
                commonElem = CommonElem(
                    serviceType = 48,
                    pbElem = msgInfo.pbEncode(),
                    businessType = businessType,
                )
            )
        )
    }

    override fun record(rawSilk: ByteArray, duration: Long) = addAsync {
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

        uploadResp.uKey.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadRecord(
                record = rawSilk,
                recordMd5 = recordMd5Bytes,
                recordSha1 = recordSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "uKey 为空，服务器可能已存在该语音，跳过上传" }

        val msgInfo = uploadResp.msgInfo
        val businessType = when (scene) {
            MessageScene.FRIEND -> 12
            MessageScene.GROUP -> 22
            MessageScene.TEMP -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        Elem(
            commonElem = CommonElem(
                serviceType = 48,
                pbElem = msgInfo.pbEncode(),
                businessType = businessType,
            )
        )
    }

    override fun video(
        raw: ByteArray,
        width: Int,
        height: Int,
        duration: Long,
        thumb: ByteArray,
        thumbFormat: ImageFormat
    ) = addAsync {
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

        uploadResp.uKey.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadVideo(
                video = raw,
                videoMd5 = videoMd5Bytes,
                videoSha1 = videoSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "uKey 为空，服务器可能已存在该视频，跳过上传" }

        uploadResp.subFileInfos.firstOrNull()?.uKey?.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadVideoThumbnail(
                thumbnail = thumb,
                thumbnailMd5 = thumbMd5Bytes,
                thumbnailSha1 = thumbSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "视频缩略图 uKey 为空，服务器可能已存在该缩略图，跳过上传" }

        val msgInfo = uploadResp.msgInfo
        val businessType = when (scene) {
            MessageScene.FRIEND -> 11
            MessageScene.GROUP -> 21
            MessageScene.TEMP -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        Elem(
            commonElem = CommonElem(
                serviceType = 48,
                pbElem = msgInfo.pbEncode(),
                businessType = businessType,
            )
        )
    }

    override fun forward(block: suspend BotForwardBlockBuilder.() -> Unit) = addAsync {
        val forwardCtx = Forward(this)
        forwardCtx.block()
        val fakeMessages = forwardCtx.build()
        val uuid = Random.nextBytes(16).let {
            "${it.sliceArray(0..3).toHexString()}-${it.sliceArray(4..5).toHexString()}-" +
                    "${it.sliceArray(6..7).toHexString()}-${it.sliceArray(8..9).toHexString()}-" +
                    it.sliceArray(10..15).toHexString()
        }
        val commonMessages = fakeMessages.map { it.commonMsg }
        this.nestedForwardTrace?.set(uuid, commonMessages)

        val resId = bot.client.callService(
            SendLongMsg,
            SendLongMsg.Req(
                scene = scene,
                peerUin = peerUin,
                peerUid = peerUid,
                messages = commonMessages,
                nestedForwardTrace = if (this.nestedForwardTrace == null)
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
                        fakeMessages.take(max(4, fakeMessages.size)).forEach {
                            add(buildJsonObject {
                                put("text", "${it.senderName}: ${it.preview}")
                            })
                        }
                    })
                    put("resid", resId)
                    put("source", "群聊的聊天记录")
                    put("summary", "查看${fakeMessages.size}条转发消息")
                    put("uniseq", uuid)
                })
            },
            desc = "[聊天记录]",
            extra = Json.encodeToString(buildJsonObject {
                put("filename", uuid)
                put("tsum", max(4, fakeMessages.size))
            }),
            prompt = "[聊天记录]",
            ver = "0.0.0.5",
            view = "contact"
        )
        val str = Json.encodeToString(lightApp)
        val buffer = Buffer()
        buffer.writeByte(0x01)
        buffer.write(ZLib.compress(str.encodeToByteArray()))

        Elem(
            lightAppElem = LightAppElem(
                bytesData = buffer.readByteArray()
            )
        )
    }

    suspend fun build(): List<Elem> = elemsList.awaitAll().flatten()

    internal class Forward(
        val ctx: MessageBuildingContext
    ) : BotForwardBlockBuilder {
        private val commonMsgList = mutableListOf<Deferred<FakeMessage>>()
        val nestedForwardTrace = mutableMapOf<String, List<CommonMessage>>()

        private fun addAsync(elem: suspend () -> FakeMessage) {
            commonMsgList.add(ctx.bot.async { elem() })
        }

        @OptIn(ExperimentalTime::class)
        override fun node(
            senderUin: Long,
            senderName: String,
            block: suspend BotOutgoingMessageBuilder.() -> Unit
        ) = addAsync {
            val subCtx = MessageBuildingContext(
                bot = ctx.bot,
                scene = ctx.scene,
                peerUin = ctx.peerUin,
                peerUid = ctx.peerUid,
                nestedForwardTrace = ctx.nestedForwardTrace ?: nestedForwardTrace,
            )
            val subBuilder = SubBuilder(subCtx)
            subBuilder.block()
            val subElems = subCtx.build()
            val preview = subBuilder.preview
            val fakeSequence = Random.nextInt(1000000, 9999999).toLong()
            FakeMessage(
                senderUin = senderUin,
                senderName = senderName,
                preview = preview,
                commonMsg = CommonMessage(
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
            )
        }

        suspend fun build() = commonMsgList.awaitAll()

        internal class FakeMessage(
            val senderUin: Long,
            val senderName: String,
            val preview: String,
            val commonMsg: CommonMessage
        )

        internal class SubBuilder(val parent: MessageBuildingContext) : BotOutgoingMessageBuilder {
            private val previewBuilder = StringBuilder()
            val preview: String
                get() = previewBuilder.toString()

            override fun text(text: String) {
                parent.text(text)
                previewBuilder.append(text)
            }

            override fun face(faceId: Int, isLarge: Boolean) {
                parent.face(faceId, isLarge)
                previewBuilder.append("[表情]")
            }

            override fun mention(uin: Long?, name: String) {
                parent.mention(uin, name)
                previewBuilder.append("@$name")
            }

            override fun reply(sequence: Long) {
                parent.reply(sequence)
            }

            override fun image(
                raw: ByteArray,
                format: ImageFormat,
                width: Int,
                height: Int,
                subType: ImageSubType,
                summary: String
            ) {
                parent.image(raw, format, width, height, subType, summary)
                previewBuilder.append(summary)
            }

            override fun record(rawSilk: ByteArray, duration: Long) {
                parent.record(rawSilk, duration)
                previewBuilder.append("[语音]")
            }

            override fun video(
                raw: ByteArray,
                width: Int,
                height: Int,
                duration: Long,
                thumb: ByteArray,
                thumbFormat: ImageFormat
            ) {
                parent.video(raw, width, height, duration, thumb, thumbFormat)
                previewBuilder.append("[视频]")
            }

            override fun forward(block: suspend BotForwardBlockBuilder.() -> Unit) {
                parent.forward(block)
                previewBuilder.append("[聊天记录]")
            }
        }
    }
}
