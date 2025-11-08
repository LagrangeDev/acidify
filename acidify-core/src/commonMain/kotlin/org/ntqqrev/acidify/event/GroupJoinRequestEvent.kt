package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 入群请求事件
 * @property groupUin 群号
 * @property notificationSeq 请求对应的通知序列号
 * @property isFiltered 请求是否被过滤（发起自风险账户）
 * @property initiatorUin 申请入群的用户 QQ 号
 * @property initiatorUid 申请入群的用户 uid
 * @property comment 申请附加信息
 */
@JsExport
class GroupJoinRequestEvent(
    val groupUin: Long,
    val notificationSeq: Long,
    val isFiltered: Boolean,
    val initiatorUin: Long,
    val initiatorUid: String,
    val comment: String
) : AcidifyEvent
