package org.ntqqrev.acidify.message

import kotlin.js.JsExport

/**
 * 接收消息段
 */
@JsExport
sealed class BotIncomingSegment {
    /**
     * 文本消息段
     * @property text 文本内容
     */
    class Text(
        val text: String,
    ) : BotIncomingSegment() {
        override fun toString(): String = text
    }

    /**
     * 提及（At）消息段
     * @property uin 被提及的用户的 QQ 号，为 `null` 表示提及了所有人（`@全体成员`）
     * @property name 被提及的用户的名称，视情况有可能是昵称 / 备注 / 群名片 / `@全体成员`
     */
    class Mention(
        val uin: Long? = null,
        val name: String,
    ) : BotIncomingSegment() {
        override fun toString(): String = name // already prefixed with '@'
    }

    /**
     * 表情消息段
     * @property faceId 表情 ID
     * @property summary 表情的文本描述
     * @property isLarge 是否为超级表情
     */
    class Face(
        val faceId: Int,
        val summary: String,
        val isLarge: Boolean,
    ) : BotIncomingSegment() {
        override fun toString(): String = summary
    }

    /**
     * 回复消息段
     * @property sequence 被回复的消息的序列号
     */
    class Reply(
        val sequence: Long,
    ) : BotIncomingSegment() {
        override fun toString(): String = "[引用消息 #$sequence] "
    }

    /**
     * 图片消息段
     * @property fileId 图片的文件 ID
     * @property width 图片的宽度
     * @property height 图片的高度
     * @property subType 图片子类型
     * @property summary 图片的文本描述
     */
    class Image(
        val fileId: String,
        val width: Int,
        val height: Int,
        val subType: ImageSubType,
        val summary: String,
    ) : BotIncomingSegment() {
        override fun toString(): String = summary
    }

    /**
     * 语音消息段
     * @property fileId 语音的文件 ID
     * @property duration 语音的时长（秒）
     */
    class Record(
        val fileId: String,
        val duration: Int,
    ) : BotIncomingSegment() {
        override fun toString(): String = "[语音 ${duration}s]"
    }

    /**
     * 视频消息段
     * @property fileId 视频的文件 ID
     * @property duration 视频的时长（秒）
     * @property width 视频的宽度
     * @property height 视频的高度
     */
    class Video(
        val fileId: String,
        val duration: Int,
        val width: Int,
        val height: Int,
    ) : BotIncomingSegment() {
        override fun toString(): String = "[视频 ${width}x${height} ${duration}s]"
    }

    /**
     * 文件消息段
     * @property fileId 文件 ID
     * @property fileName 文件名称
     * @property fileSize 文件大小（字节）
     * @property fileHash 文件的 TriSHA1 哈希值，仅在私聊文件中存在
     */
    class File(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val fileHash: String? = null,
    ) : BotIncomingSegment() {
        override fun toString(): String = "[文件 $fileName]"
    }

    /**
     * 转发消息段
     * @property resId 转发消息的资源 ID
     */
    class Forward(
        val resId: String,
    ) : BotIncomingSegment() {
        override fun toString(): String = "[转发消息]"
    }

    /**
     * 市场表情消息段
     * @property url 表情的图像 URL
     * @property summary 表情的文本描述
     */
    class MarketFace(
        val url: String,
        val summary: String,
    ) : BotIncomingSegment() {
        override fun toString(): String = summary
    }

    /**
     * 小程序消息段
     * @property appName 小程序 App Name
     * @property jsonPayload 小程序的 JSON 负载
     */
    class LightApp(
        val appName: String,
        val jsonPayload: String,
    ) : BotIncomingSegment() {
        override fun toString(): String = "[卡片消息 $appName]"
    }
}