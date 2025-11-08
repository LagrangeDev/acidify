package org.ntqqrev.acidify.message

import kotlin.js.JsExport

/**
 * 群精华消息段
 */
@JsExport
sealed class BotEssenceSegment {
    /**
     * 文本消息段
     * @property text 文本内容
     */
    data class Text(
        val text: String
    ) : BotEssenceSegment() {
        override fun toString(): String = text
    }

    /**
     * 表情消息段
     * @property faceId 表情 ID
     */
    data class Face(
        val faceId: Int
    ) : BotEssenceSegment() {
        override fun toString(): String = "[表情 $faceId]"
    }

    /**
     * 图片消息段
     * @property imageUrl 图片 URL
     */
    data class Image(
        val imageUrl: String
    ) : BotEssenceSegment() {
        override fun toString(): String = "[图片]"
    }

    /**
     * 视频消息段
     * @property thumbnailUrl 视频封面 URL
     */
    data class Video(
        val thumbnailUrl: String
    ) : BotEssenceSegment() {
        override fun toString(): String = "[视频]"
    }
}

