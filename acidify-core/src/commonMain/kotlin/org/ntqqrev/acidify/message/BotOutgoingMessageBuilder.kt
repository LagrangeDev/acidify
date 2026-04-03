package org.ntqqrev.acidify.message

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import org.ntqqrev.acidify.common.AcidifyDsl
import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.common.MediaSource.Companion.toMediaSource
import kotlin.js.JsName

/**
 * 构建发送消息
 */
@AcidifyDsl
class BotOutgoingMessageBuilder {
    val segments = mutableListOf<BotOutgoingSegment>()

    /**
     * 添加文本消息段
     * @param text 文本内容
     */
    fun text(text: String) {
        segments.add(BotOutgoingSegment.Text(text))
    }

    /**
     * 添加提及（At）消息段
     * @param uin 被提及的用户的 QQ 号，为 `null` 表示提及了所有人（`@全体成员`）
     * @param name 被提及的用户的外显名称（仅在电脑端可见）
     */
    fun mention(uin: Long?, name: String) {
        segments.add(BotOutgoingSegment.Mention(uin, name))
    }

    /**
     * 添加表情消息段
     * @param faceId 表情 ID
     * @param isLarge 是否为超级表情
     */
    fun face(faceId: Int, isLarge: Boolean = false) {
        segments.add(BotOutgoingSegment.Face(faceId, isLarge))
    }

    /**
     * 添加回复消息段
     * @param sequence 被回复的消息的序列号
     */
    fun reply(sequence: Long) {
        segments.add(BotOutgoingSegment.Reply(sequence))
    }

    /**
     * 添加图片消息段
     * @param raw 图片数据
     * @param format 图片格式
     * @param width 图片宽度（像素）
     * @param height 图片高度（像素）
     * @param subType 图片子类型
     * @param summary 图片的文本描述
     */
    fun image(
        raw: MediaSource,
        format: ImageFormat,
        width: Int,
        height: Int,
        subType: ImageSubType = ImageSubType.NORMAL,
        summary: String = "[图片]"
    ) {
        segments.add(
            BotOutgoingSegment.Image(
                raw,
                format,
                width,
                height,
                subType,
                summary
            )
        )
    }

    fun image(
        raw: ByteArray,
        format: ImageFormat,
        width: Int,
        height: Int,
        subType: ImageSubType = ImageSubType.NORMAL,
        summary: String = "[图片]"
    ) = image(
        raw = raw.toMediaSource(),
        format = format,
        width = width,
        height = height,
        subType = subType,
        summary = summary,
    )

    /**
     * 添加语音消息段
     * @param rawSilk Silk 格式的语音数据
     * @param duration 语音时长（秒）
     */
    fun record(
        rawSilk: MediaSource,
        duration: Long
    ) {
        segments.add(BotOutgoingSegment.Record(rawSilk, duration))
    }

    fun record(
        rawSilk: ByteArray,
        duration: Long
    ) = record(rawSilk.toMediaSource(), duration)

    /**
     * 添加视频消息段
     * @param raw 视频数据
     * @param width 视频宽度（像素）
     * @param height 视频高度（像素）
     * @param duration 视频时长（秒）
     * @param thumb 视频缩略图数据
     * @param thumbFormat 视频缩略图格式
     */
    fun video(
        raw: MediaSource,
        width: Int,
        height: Int,
        duration: Long,
        thumb: MediaSource,
        thumbFormat: ImageFormat
    ) {
        segments.add(
            BotOutgoingSegment.Video(
                raw,
                width,
                height,
                duration,
                thumb,
                thumbFormat
            )
        )
    }

    fun video(
        raw: ByteArray,
        width: Int,
        height: Int,
        duration: Long,
        thumb: ByteArray,
        thumbFormat: ImageFormat
    ) = video(
        raw = raw.toMediaSource(),
        width = width,
        height = height,
        duration = duration,
        thumb = thumb.toMediaSource(),
        thumbFormat = thumbFormat,
    )

    /**
     * 添加合并转发消息段
     * @param block 构建合并转发消息
     */
    inline fun forward(block: Forward.() -> Unit) {
        val forwardBuilder = Forward()
        forwardBuilder.block()
        segments.add(BotOutgoingSegment.Forward(forwardBuilder.nodes))
    }

    @AcidifyDsl
    class Forward {
        val nodes = mutableListOf<BotOutgoingSegment.Forward.Node>()

        /**
         * 添加一个伪造合并转发消息
         * @param senderUin 该消息的发送者 QQ 号
         * @param senderName 该消息的发送者昵称
         * @param block 构建该消息的内容
         */
        inline fun node(
            senderUin: Long,
            senderName: String,
            block: BotOutgoingMessageBuilder.() -> Unit
        ) {
            val messageBuilder = BotOutgoingMessageBuilder()
            messageBuilder.block()
            nodes.add(
                BotOutgoingSegment.Forward.Node(
                    senderUin,
                    senderName,
                    messageBuilder.segments
                )
            )
        }
    }

    /**
     * 添加小程序消息段
     * @param jsonPayload 小程序消息的 JSON 负载
     */
    fun lightApp(jsonPayload: String) {
        segments.add(BotOutgoingSegment.LightApp(jsonPayload))
    }

    /**
     * 添加小程序消息段
     * @param jsonPayload 小程序消息的 JSON 负载
     */
    @JsName("lightAppFromJsonElem")
    fun lightApp(jsonPayload: JsonElement) = lightApp(Json.encodeToString(jsonPayload))

    /**
     * 通过构建 JSON 对象的方式添加小程序消息段
     * @param block 构建 JSON 对象
     */
    @JsName("lightAppFromJsonBuilder")
    inline fun lightApp(block: JsonObjectBuilder.() -> Unit) = lightApp(
        buildJsonObject {
            block()
        }
    )

    operator fun String.unaryPlus() = text(this)
}