package org.ntqqrev.acidify.message.internal

import dev.karmakrafts.kompress.Inflater
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.ntqqrev.acidify.internal.json.message.ForwardJsonPayload
import org.ntqqrev.acidify.internal.json.message.ForwardXmlPayload
import org.ntqqrev.acidify.internal.json.message.lightAppJsonModule
import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import org.ntqqrev.acidify.internal.proto.message.elem.SourceMsg
import org.ntqqrev.acidify.internal.proto.message.extra.*
import org.ntqqrev.acidify.internal.proto.message.media.MsgInfo
import org.ntqqrev.acidify.internal.util.BinaryReader
import org.ntqqrev.acidify.internal.util.Prefix
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.message.BotIncomingSegment
import org.ntqqrev.acidify.message.ImageSubType
import org.ntqqrev.acidify.message.MessageScene
import kotlin.math.min

internal interface IncomingSegmentFactory<T : BotIncomingSegment> {
    fun tryParse(ctx: MessageParsingContext): T?

    companion object {
        val factories = listOf(
            Text,
            Mention,
            Face,
            Reply,
            Image,
            Record,
            Video,
            File,
            Forward,
            MarketFace,
            LightApp,
            Markdown,
        )
    }

    object Text : IncomingSegmentFactory<BotIncomingSegment.Text> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Text? {
            val text = ctx.tryPeekType { text }
                ?.takeIf { it.pbReserve.isEmpty() }
                ?: return null
            ctx.consume()

            // Markdown message layout: text + commonElem(45,1)
            if (ctx.hasNext()) {
                val elem = ctx.tryPeekType { commonElem }
                if (elem?.serviceType == 45 && elem.businessType == 1) return null
            }

            return BotIncomingSegment.Text(
                text = text.textMsg
            )
        }
    }

    object Mention : IncomingSegmentFactory<BotIncomingSegment.Mention> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Mention? {
            val at = ctx.tryPeekType { text }
                ?.takeIf { it.pbReserve.isNotEmpty() }
                ?: return null
            ctx.consume()
            val attr = at.pbReserve.pbDecode<TextResvAttr>()
            return when (attr.atType) {
                1 -> BotIncomingSegment.Mention( // Mention all
                    uin = null,
                    uid = null,
                    name = at.textMsg
                )

                2 -> BotIncomingSegment.Mention( // specific user
                    uin = attr.atMemberUin,
                    uid = attr.atMemberUid,
                    name = at.textMsg
                )

                else -> null
            }
        }
    }

    object Face : IncomingSegmentFactory<BotIncomingSegment.Face> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Face? {
            ctx.tryPeekType { face }?.let { face ->
                ctx.consume()
                val detail = ctx.bot.faceDetailMap[face.index.toString()]
                return BotIncomingSegment.Face(
                    faceId = face.index,
                    summary = "[${detail?.qDes?.removePrefix("/") ?: "表情"}]",
                    isLarge = false,
                )
            }

            ctx.tryPeekType { commonElem }?.let { common ->
                val serviceType = common.serviceType
                if (serviceType == 33) {
                    ctx.consume()
                    val extra = common.pbElem.pbDecode<QSmallFaceExtra>()
                    val faceId = extra.faceId
                    val detail = ctx.bot.faceDetailMap[faceId.toString()]
                    return BotIncomingSegment.Face(
                        faceId = faceId,
                        summary = "[${
                            (detail?.qDes ?: extra.text.takeIf { it.isNotEmpty() })
                                ?.removePrefix("/")
                                ?: "表情"
                        }]",
                        isLarge = false,
                    )
                }

                if (serviceType == 37) {
                    ctx.consume()
                    val extra = common.pbElem.pbDecode<QBigFaceExtra>()
                    val faceId = extra.faceId
                    val detail = ctx.bot.faceDetailMap[faceId.toString()]
                    if (ctx.hasNext()) {
                        ctx.tryPeekType { text }?.let { ctx.skip() }
                    }
                    return BotIncomingSegment.Face(
                        faceId = faceId,
                        summary = "[${
                            (detail?.qDes ?: extra.preview.takeIf { it.isNotEmpty() })
                                ?.removePrefix("/")
                                ?: "超级表情"
                        }]",
                        isLarge = true,
                    )
                }
            }
            return null
        }
    }

    object Reply : IncomingSegmentFactory<BotIncomingSegment.Reply> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Reply? {
            val reply = ctx.tryPeekType { srcMsg }
                ?: return null
            ctx.consume()
            if (ctx.remainingCount >= 2) {
                if (ctx.tryPeekType { generalFlags } != null) {
                    when (ctx.scene) {
                        MessageScene.FRIEND -> ctx.skip(2)
                        // generalFlags + elemFlags2
                        MessageScene.GROUP -> ctx.skip(min(4, ctx.remainingCount))
                        // generalFlags + elemFlags2 + mention + text(' ')
                        else -> {}
                    }
                } else if (ctx.tryPeekType { text }?.pbReserve?.isNotEmpty() == true) {
                    if (ctx.scene == MessageScene.GROUP) {
                        ctx.skip(2) // mention + text(' ')
                    }
                }
            }
            return BotIncomingSegment.Reply(
                sequence = when (ctx.scene) {
                    MessageScene.GROUP -> reply.origSeqs.firstOrNull() ?: 0L
                    else -> if (reply.pbReserve.isNotEmpty()) {
                        reply.pbReserve.pbDecode<SourceMsg.PbReserve>().friendSequence
                    } else {
                        0L
                    }
                },
                senderUin = reply.senderUin,
                senderName = reply.srcMsg?.let {
                    val commonMsg = it.pbDecode<CommonMessage>()
                    commonMsg.routingHead.group.groupCard
                },
                timestamp = reply.time,
                segments = ctx.bot.buildSegments(
                    elems = reply.elems,
                    scene = ctx.scene,
                ),
            )
        }
    }

    object Image : IncomingSegmentFactory<BotIncomingSegment.Image> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Image? {
            val common = ctx.tryPeekType { commonElem } ?: return null
            val businessType = common.businessType
            if (businessType != 10 && businessType != 20) return null
            ctx.consume()
            val msgInfo = common.pbElem.pbDecode<MsgInfo>()
            val index = msgInfo.msgInfoBody.firstOrNull()?.index ?: return null
            val info = index.info
            val picBiz = msgInfo.extBizInfo.pic
            return BotIncomingSegment.Image(
                fileId = index.fileUuid,
                width = info.width,
                height = info.height,
                subType = when (picBiz.bizType) {
                    0 -> ImageSubType.NORMAL
                    else -> ImageSubType.STICKER
                },
                summary = picBiz.textSummary.ifEmpty { "[图片]" },
            )
        }
    }

    object Record : IncomingSegmentFactory<BotIncomingSegment.Record> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Record? {
            val common = ctx.tryPeekType { commonElem } ?: return null
            val businessType = common.businessType
            if (businessType != 12 && businessType != 22) return null
            ctx.consume()
            val msgInfo = common.pbElem.pbDecode<MsgInfo>()
            val index = msgInfo.msgInfoBody.firstOrNull()?.index ?: return null
            val info = index.info
            return BotIncomingSegment.Record(
                fileId = index.fileUuid,
                duration = info.time,
            )
        }
    }

    object Video : IncomingSegmentFactory<BotIncomingSegment.Video> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Video? {
            val common = ctx.tryPeekType { commonElem } ?: return null
            val businessType = common.businessType
            if (businessType != 11 && businessType != 21) return null
            ctx.consume()
            val msgInfo = common.pbElem.pbDecode<MsgInfo>()
            val videoIndex = msgInfo.msgInfoBody.firstOrNull()?.index ?: return null
            val videoInfo = videoIndex.info
            return BotIncomingSegment.Video(
                fileId = videoIndex.fileUuid,
                duration = videoInfo.time,
                width = videoInfo.width,
                height = videoInfo.height,
            )
        }
    }

    object File : IncomingSegmentFactory<BotIncomingSegment.File> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.File? {
            if (ctx.scene != MessageScene.GROUP) return null
            val trans = ctx.tryPeekType { transElemInfo } ?: return null
            if (trans.elemType != 24) return null
            ctx.consume()
            val payload = trans.elemValue
            val reader = BinaryReader(payload)
            reader.readByte() // skip 1 byte (same as Skip(1))
            val data = reader.readPrefixedBytes(Prefix.UINT_16)
            val extra = data.pbDecode<GroupFileExtra>()
            val info = extra.inner.info
            return BotIncomingSegment.File(
                fileId = info.fileId,
                fileName = info.fileName,
                fileSize = info.fileSize,
            )
        }
    }

    object Forward : IncomingSegmentFactory<BotIncomingSegment.Forward> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Forward? {
            ctx.tryPeekType { richMsg }?.let { forward ->
                ctx.consume()
                val bytesTemplate1 = forward.bytesTemplate1
                val xml = Inflater.inflate(
                    bytesTemplate1.sliceArray(1 until bytesTemplate1.size),
                    raw = false
                ).decodeToString()
                val body = ForwardXmlPayload.xmlModule.decodeFromString<ForwardXmlPayload>(xml)
                val titles = body.items[0].titles
                return BotIncomingSegment.Forward(
                    resId = body.resId,
                    title = titles[0].text,
                    preview = titles.drop(1).map { it.text },
                    summary = body.items[0].summaries[0].text,
                )
            }

            ctx.tryPeekType { lightAppElem }?.let { elem ->
                val compressed = elem.bytesData
                val json = Inflater.inflate(
                    compressed.sliceArray(1 until compressed.size),
                    raw = false
                ).decodeToString()
                val appName = Json.parseToJsonElement(json)
                    .jsonObject["app"]
                    ?.jsonPrimitive
                    ?.content
                    ?: return null
                if (appName != "com.tencent.multimsg") return null
                ctx.consume()

                val forwardPayload = lightAppJsonModule.decodeFromString<ForwardJsonPayload>(json)
                val detail = forwardPayload.meta.detail
                return BotIncomingSegment.Forward(
                    resId = detail.resid,
                    title = detail.source,
                    preview = detail.news.map { it.text },
                    summary = detail.summary,
                )
            }

            return null
        }
    }

    object MarketFace : IncomingSegmentFactory<BotIncomingSegment.MarketFace> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.MarketFace? {
            val market = ctx.tryPeekType { marketFace } ?: return null
            ctx.consume()
            if (ctx.tryPeekType { text }?.textMsg == market.summary) {
                ctx.skip()
            }
            val faceIdHex = market.faceId.toHexString()
            return BotIncomingSegment.MarketFace(
                url = "https://gxh.vip.qq.com/club/item/parcel/item/${faceIdHex.take(2)}/$faceIdHex/raw300.gif",
                summary = market.summary,
                emojiId = faceIdHex,
                emojiPackageId = market.tabId,
                key = market.key,
            )
        }
    }

    object LightApp : IncomingSegmentFactory<BotIncomingSegment.LightApp> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.LightApp? {
            val elem = ctx.tryPeekType { lightAppElem } ?: return null
            ctx.consume()
            if (ctx.tryPeekType { text } != null) {
                ctx.skip()
            }
            val compressed = elem.bytesData
            val json = Inflater.inflate(
                compressed.sliceArray(1 until compressed.size),
                raw = false
            ).decodeToString()
            val appName = Json.parseToJsonElement(json)
                .jsonObject["app"]
                ?.jsonPrimitive
                ?.content
                ?: return null
            return BotIncomingSegment.LightApp(
                appName = appName,
                jsonPayload = json,
            )
        }
    }

    object Markdown : IncomingSegmentFactory<BotIncomingSegment.Markdown> {
        override fun tryParse(ctx: MessageParsingContext): BotIncomingSegment.Markdown? {
            val elem = ctx.tryPeekType { commonElem } ?: return null
            if (elem.serviceType != 45 || elem.businessType != 1) return null
            ctx.consume()
            val markdown = elem.pbElem.pbDecode<MarkdownExtra>()
            return BotIncomingSegment.Markdown(
                content = markdown.content
            )
        }
    }
}
