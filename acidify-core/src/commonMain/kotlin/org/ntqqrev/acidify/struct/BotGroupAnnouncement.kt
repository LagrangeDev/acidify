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
 * @property showEditCard 是否引导新成员编辑群名片
 * @property showTipWindow 是否在弹窗中展示公告
 * @property confirmRequired 是否需要群成员显式确认
 * @property isPinned 是否置顶
 * @property showToNewMember 是否向新成员展示
 */
@JsExport
data class BotGroupAnnouncement internal constructor(
    val groupUin: Long,
    val announcementId: String,
    val senderId: Long,
    val time: Long,
    val content: String,
    val imageUrl: String?,
    val showEditCard: Boolean,
    val showTipWindow: Boolean,
    val confirmRequired: Boolean,
    val isPinned: Boolean,
    val showToNewMember: Boolean,
)