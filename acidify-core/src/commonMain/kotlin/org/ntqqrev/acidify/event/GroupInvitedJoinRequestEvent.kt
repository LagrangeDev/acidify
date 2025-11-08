package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群成员邀请他人入群请求事件
 * @property groupUin 群号
 * @property notificationSeq 请求对应的通知序列号
 * @property initiatorUin 邀请者 QQ 号
 * @property initiatorUid 邀请者 uid
 * @property targetUserUin 被邀请者 QQ 号
 * @property targetUserUid 被邀请者 uid
 */
@JsExport
class GroupInvitedJoinRequestEvent(
    val groupUin: Long,
    val notificationSeq: Long,
    val initiatorUin: Long,
    val initiatorUid: String,
    val targetUserUin: Long,
    val targetUserUid: String
) : AcidifyEvent
