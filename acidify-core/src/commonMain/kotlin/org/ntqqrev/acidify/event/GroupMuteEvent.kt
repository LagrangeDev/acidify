package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群禁言事件
 * @property groupUin 群号
 * @property userUin 发生变更的用户 QQ 号
 * @property userUid 发生变更的用户 uid
 * @property operatorUin 操作者 QQ 号
 * @property operatorUid 操作者 uid
 * @property duration 禁言时长（秒），为 0 表示取消禁言
 */
@JsExport
class GroupMuteEvent(
    val groupUin: Long,
    val userUin: Long,
    val userUid: String,
    val operatorUin: Long,
    val operatorUid: String,
    val duration: Int
) : AcidifyEvent