package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * 群公告信息
 * @property groupUin 群号
 * @property announcementId 公告 ID
 * @property senderId 发送者 QQ 号
 * @property time Unix 时间戳（秒）
 * @property content 公告内容
 * @property imageUrl 公告图片 URL，可能为 `null`
 */
@JsExport
data class BotGroupAnnouncement(
    val groupUin: Long,
    val announcementId: String,
    val senderId: Long,
    val time: Long,
    val content: String,
    val imageUrl: String? = null
)