package org.ntqqrev.acidify.message

import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.common.MediaSource.Companion.toMediaSource
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * 发送消息段
 */
@JsExport
sealed class BotOutgoingSegment {
    /**
     * 文本消息段
     * @property text 文本内容
     */
    data class Text(
        val text: String,
    ) : BotOutgoingSegment() {
        override fun toString(): String = text
    }

    /**
     * 提及（At）消息段
     * @property uin 被提及的用户的 QQ 号，为 `null` 表示提及了所有人（`@全体成员`）
     * @property name 被提及的用户的外显名称（仅在电脑端可见）
     */
    data class Mention(
        val uin: Long? = null,
        val name: String,
    ) : BotOutgoingSegment() {
        override fun toString(): String = name // already prefixed with '@'
    }

    /**
     * 表情消息段
     * @property faceId 表情 ID
     * @property isLarge 是否为超级表情
     */
    data class Face(
        val faceId: Int,
        val isLarge: Boolean,
    ) : BotOutgoingSegment() {
        override fun toString(): String = "[表情]"
    }

    /**
     * 回复消息段
     * @property sequence 被回复的消息的序列号
     */
    data class Reply(
        val sequence: Long,
    ) : BotOutgoingSegment() {
        override fun toString(): String = "[引用消息]"
    }

    /**
     * 图片消息段
     * @property raw 图片数据
     * @property format 图片格式
     * @property width 图片宽度（像素）
     * @property height 图片高度（像素）
     * @property subType 图片子类型
     * @property summary 图片的文本描述
     */
    data class Image(
        val raw: MediaSource,
        val format: ImageFormat,
        val width: Int,
        val height: Int,
        val subType: ImageSubType,
        val summary: String,
    ) : BotOutgoingSegment() {
        @JsName("fromBytes")
        constructor(
            raw: ByteArray,
            format: ImageFormat,
            width: Int,
            height: Int,
            subType: ImageSubType,
            summary: String,
        ) : this(
            raw = raw.toMediaSource(),
            format = format,
            width = width,
            height = height,
            subType = subType,
            summary = summary,
        )

        override fun toString(): String = summary
    }

    /**
     * 语音消息段
     * @property rawSilk Silk 格式的语音数据
     * @property duration 语音时长（秒）
     */
    data class Record(
        val rawSilk: MediaSource,
        val duration: Long,
    ) : BotOutgoingSegment() {
        @JsName("fromBytes")
        constructor(
            rawSilk: ByteArray,
            duration: Long,
        ) : this(
            rawSilk = rawSilk.toMediaSource(),
            duration = duration,
        )

        override fun toString(): String = "[语音 ${duration}s]"
    }

    /**
     * 视频消息段
     * @property raw 视频数据
     * @property width 视频宽度（像素）
     * @property height 视频高度（像素）
     * @property duration 视频时长（秒）
     * @property thumb 视频缩略图数据
     * @property thumbFormat 视频缩略图格式
     */
    data class Video(
        val raw: MediaSource,
        val width: Int,
        val height: Int,
        val duration: Long,
        val thumb: MediaSource,
        val thumbFormat: ImageFormat,
    ) : BotOutgoingSegment() {
        @JsName("fromBytes")
        constructor(
            raw: ByteArray,
            width: Int,
            height: Int,
            duration: Long,
            thumb: ByteArray,
            thumbFormat: ImageFormat,
        ) : this(
            raw = raw.toMediaSource(),
            width = width,
            height = height,
            duration = duration,
            thumb = thumb.toMediaSource(),
            thumbFormat = thumbFormat,
        )

        override fun toString(): String = "[视频 ${width}x${height} ${duration}s]"
    }

    /**
     * 合并转发消息段
     *
     * 一条合并转发聊天记录的预览信息示例如下：
     *
     * ```
     * 群聊的聊天记录      // title
     * Salt: [动画表情]   // preview[0]
     * Milk: [图片]      // preview[1]
     * Shama: [视频]     // preview[2]
     * ---
     * 查看3条转发消息     // summary
     * ```
     *
     * @property nodes 转发的消息节点列表
     * @property title 转发消息的标题
     * @property preview 转发消息的预览文本列表
     * @property summary 转发消息的摘要文本
     * @property prompt 转发消息的文本描述，类似 [Image.summary] 的外显文本
     */
    data class Forward(
        val nodes: List<Node>,
        val title: String = "群聊的聊天记录",
        val preview: List<String> = nodes.take(4).map {
            it.senderName + ": " + it.segments.joinToString("")
        },
        val summary: String = "查看${nodes.size}条转发消息",
        val prompt: String = "[聊天记录]"
    ) : BotOutgoingSegment() {
        override fun toString(): String = prompt

        /**
         * 转发消息中的单条消息节点
         * @property senderUin 发送者 QQ 号
         * @property senderName 发送者名称
         * @property segments 消息内容
         */
        data class Node(
            val senderUin: Long,
            val senderName: String,
            val segments: List<BotOutgoingSegment>,
        )
    }

    /**
     * 小程序消息段
     * @property jsonPayload 小程序的 JSON 负载
     */
    data class LightApp(
        val jsonPayload: String
    ) : BotOutgoingSegment()
}