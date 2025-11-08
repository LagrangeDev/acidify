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
import org.ntqqrev.acidify.internal.crypto.hash.MD5
import org.ntqqrev.acidify.internal.packet.message.*
import org.ntqqrev.acidify.internal.packet.message.elem.*
import org.ntqqrev.acidify.internal.packet.message.extra.QBigFaceExtra
import org.ntqqrev.acidify.internal.packet.message.extra.QSmallFaceExtra
import org.ntqqrev.acidify.internal.packet.message.extra.SourceMsgResvAttr
import org.ntqqrev.acidify.internal.packet.message.extra.TextResvAttr
import org.ntqqrev.acidify.internal.packet.message.misc.ForwardLightAppPayload
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.message.RichMediaUpload
import org.ntqqrev.acidify.internal.service.message.SendLongMsg
import org.ntqqrev.acidify.internal.util.sha1
import org.ntqqrev.acidify.message.BotOutgoingMessageBuilder
import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.message.ImageSubType
import org.ntqqrev.acidify.message.MessageScene
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class MessageBuildingContext(
    val bot: Bot,
    val scene: MessageScene,
    val peerUin: Long,
    val peerUid: String,
    private val nestedForwardTrace: MutableMap<String, List<PbObject<CommonMessage>>>? = null
) : BotOutgoingMessageBuilder {
    private val logger = bot.createLogger(this)
    private val elemsList = mutableListOf<Deferred<List<PbObject<Elem>>>>()

    private fun addAsync(elem: suspend () -> PbObject<Elem>) {
        elemsList.add(bot.async { listOf(elem()) })
    }

    private fun addMultipleAsync(elems: suspend () -> List<PbObject<Elem>>) {
        elemsList.add(bot.async { elems() })
    }

    override fun text(text: String) = addAsync {
        Elem {
            it[this.text] = Text {
                it[textMsg] = text
            }
        }
    }

    override fun mention(uin: Long?, name: String) = addAsync {
        Elem {
            it[text] = Text {
                it[textMsg] = "@$name"
                it[pbReserve] = TextResvAttr {
                    it[atType] = if (uin == null) 1 else 2  // 1 for @all, 2 for @specific
                    if (uin != null) {
                        it[atMemberUin] = uin
                        it[atMemberUid] = bot.getUidByUin(uin)
                    }
                }.toByteArray()
            }
        }
    }

    override fun face(faceId: Int, isLarge: Boolean) = addAsync {
        val faceDetail = bot.faceDetailMap[faceId.toString()]
            ?: throw NoSuchElementException("要发送的表情 ID 不存在: $faceId")

        if (isLarge) {
            Elem {
                it[commonElem] = CommonElem {
                    it[serviceType] = 37
                    it[pbElem] = QBigFaceExtra {
                        it[aniStickerPackId] = faceDetail.aniStickerPackId.toString()
                        it[aniStickerId] = faceDetail.aniStickerId.toString()
                        it[this.faceId] = faceId
                        it[field4] = 1
                        it[aniStickerType] = faceDetail.aniStickerType
                        it[field6] = ""
                        it[preview] = faceDetail.qDes
                        it[field9] = 1
                    }.toByteArray()
                    it[businessType] = faceDetail.aniStickerType
                }
            }
        }

        if (faceId >= 260) {
            Elem {
                it[commonElem] = CommonElem {
                    it[serviceType] = 33
                    it[pbElem] = QSmallFaceExtra {
                        it[this.faceId] = faceId
                        it[text] = faceDetail.qDes
                        it[compatText] = faceDetail.qDes
                    }.toByteArray()
                    it[businessType] = faceDetail.aniStickerType
                }
            }
        }

        Elem {
            it[face] = Face {
                it[index] = faceId
            }
        }
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

        val srcMsgElem = Elem {
            it[srcMsg] = SourceMsg {
                it[origSeqs] = listOf(
                    if (scene == MessageScene.FRIEND) replied.clientSequence
                    else replied.sequence
                )
                it[senderUin] = 0L
                it[time] = replied.timestamp
                it[flag] = 0
                it[elems] = emptyList() // 客户端会自行获取原始消息内容
                it[pbReserve] = SourceMsgResvAttr {
                    it[oriMsgType] = 2
                    it[sourceMsgId] = replied.messageUid
                    it[senderUid] = replied.senderUid
                }.toByteArray()
            }
        }

        // 群消息需要额外添加 @ 提及（客户端会根据 uid 显示昵称）
        if (scene == MessageScene.GROUP) {
            listOf(
                srcMsgElem,
                Elem {
                    it[text] = Text {
                        it[textMsg] = "@${replied.senderUin}" // 显示 uin，客户端可能会替换为昵称
                        it[pbReserve] = TextResvAttr {
                            it[atType] = 2
                            it[atMemberUin] = replied.senderUin
                            it[atMemberUid] = replied.senderUid
                        }.toByteArray()
                    }
                },
                Elem {
                    it[text] = Text {
                        it[textMsg] = " "
                    }
                }
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

        uploadResp.get { uKey }.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadImage(
                image = raw,
                imageMd5 = imageMd5Bytes,
                imageSha1 = imageSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "uKey 为空，服务器可能已存在该图片，跳过上传" }

        val msgInfo = uploadResp.get { msgInfo }
        val businessType = when (scene) {
            MessageScene.FRIEND -> 10
            MessageScene.GROUP -> 20
            else -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        listOf(
            Elem { e ->
                when (scene) {
                    MessageScene.FRIEND -> e[notOnlineImage] = NotOnlineImage(uploadResp.get { compatQMsg })
                    MessageScene.GROUP -> e[customFace] = CustomFace(uploadResp.get { compatQMsg })
                    else -> {}
                }
            },
            Elem {
                it[commonElem] = CommonElem {
                    it[serviceType] = 48
                    it[pbElem] = msgInfo.toByteArray()
                    it[this.businessType] = businessType
                }
            }
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

        uploadResp.get { uKey }.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadRecord(
                record = rawSilk,
                recordMd5 = recordMd5Bytes,
                recordSha1 = recordSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "uKey 为空，服务器可能已存在该语音，跳过上传" }

        val msgInfo = uploadResp.get { msgInfo }
        val businessType = when (scene) {
            MessageScene.FRIEND -> 12
            MessageScene.GROUP -> 22
            else -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        Elem {
            it[commonElem] = CommonElem {
                it[serviceType] = 48
                it[pbElem] = msgInfo.toByteArray()
                it[this.businessType] = businessType
            }
        }
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

        uploadResp.get { uKey }.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadVideo(
                video = raw,
                videoMd5 = videoMd5Bytes,
                videoSha1 = videoSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "uKey 为空，服务器可能已存在该视频，跳过上传" }

        uploadResp.get { subFileInfos }[0].get { uKey }.takeIf { it.isNotEmpty() }?.let {
            bot.client.highwayContext.uploadVideoThumbnail(
                thumbnail = thumb,
                thumbnailMd5 = thumbMd5Bytes,
                thumbnailSha1 = thumbSha1Bytes,
                uploadResp = uploadResp,
                messageScene = scene
            )
        } ?: logger.d { "视频缩略图 uKey 为空，服务器可能已存在该缩略图，跳过上传" }

        val msgInfo = uploadResp.get { msgInfo }
        val businessType = when (scene) {
            MessageScene.FRIEND -> 11
            MessageScene.GROUP -> 21
            else -> throw IllegalArgumentException("不支持的消息场景: $scene")
        }

        Elem {
            it[commonElem] = CommonElem {
                it[serviceType] = 48
                it[pbElem] = msgInfo.toByteArray()
                it[this.businessType] = businessType
            }
        }
    }

    override fun forward(block: suspend org.ntqqrev.acidify.message.BotForwardBlockBuilder.() -> Unit) = addAsync {
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

        val lightApp = ForwardLightAppPayload(
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

        Elem {
            it[lightAppElem] = LightAppElem {
                it[bytesData] = buffer.readByteArray()
            }
        }
    }

    suspend fun build(): List<PbObject<Elem>> = elemsList.awaitAll().flatten()

    internal class Forward(
        val ctx: MessageBuildingContext
    ) : org.ntqqrev.acidify.message.BotForwardBlockBuilder {
        private val commonMsgList = mutableListOf<Deferred<FakeMessage>>()
        val nestedForwardTrace = mutableMapOf<String, List<PbObject<CommonMessage>>>()

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
            FakeMessage(
                senderUin = senderUin,
                senderName = senderName,
                preview = preview,
                commonMsg = CommonMessage {
                    it[routingHead] = RoutingHead {
                        it[fromUin] = senderUin
                        it[toUid] = ctx.bot.uid
                        when (ctx.scene) {
                            MessageScene.FRIEND -> it[commonC2C] = RoutingHead.CommonC2C {
                                it[name] = senderName
                            }

                            MessageScene.GROUP -> it[group] = RoutingHead.CommonGroup {
                                it[groupCode] = ctx.peerUin
                                it[groupCard] = senderName
                                it[groupCardType] = 2
                            }

                            else -> {}
                        }
                    }
                    it[contentHead] = ContentHead {
                        it[type] = when (ctx.scene) {
                            MessageScene.FRIEND -> PushMsgType.FriendMessage.value
                            MessageScene.GROUP -> PushMsgType.GroupMessage.value
                            MessageScene.TEMP -> PushMsgType.TempMessage.value
                        }
                        it[random] = Random.nextInt()
                        it[sequence] = Random.nextInt(1000000, 9999999).toLong()
                        it[time] = Clock.System.now().epochSeconds
                        it[clientSequence] = it[sequence]
                        it[msgUid] = Random.nextLong(1000000000000, 9999999999999)
                        it[forwardExt] = ContentHead.Forward {
                            it[field3] = 2
                            it[avatar] = "https://q.qlogo.cn/headimg_dl?dst_uin=$senderUin&spec=640&img_type=jpg"
                        }
                    }
                    it[messageBody] = MessageBody {
                        it[richText] = RichText {
                            it[elems] = subElems
                        }
                    }
                }
            )
        }

        suspend fun build() = commonMsgList.awaitAll()

        internal class FakeMessage(
            val senderUin: Long,
            val senderName: String,
            val preview: String,
            val commonMsg: PbObject<CommonMessage>
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

            override fun forward(block: suspend org.ntqqrev.acidify.message.BotForwardBlockBuilder.() -> Unit) {
                parent.forward(block)
                previewBuilder.append("[聊天记录]")
            }
        }
    }
}