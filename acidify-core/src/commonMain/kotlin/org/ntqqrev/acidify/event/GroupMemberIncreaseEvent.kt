package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群成员增加事件
 * @property groupUin 群号
 * @property userUin 发生变更的用户 QQ 号
 * @property userUid 发生变更的用户 uid
 * @property operatorUin 管理员 QQ 号，如果是管理员同意入群
 * @property operatorUid 管理员 uid，如果是管理员同意入群
 * @property invitorUin 邀请者 QQ 号，如果是邀请入群
 * @property invitorUid 邀请者 uid，如果是邀请入群
 */
@JsExport
class GroupMemberIncreaseEvent(
    val groupUin: Long,
    val userUin: Long,
    val userUid: String,
    val operatorUin: Long?,
    val operatorUid: String?,
    val invitorUin: Long?,
    val invitorUid: String?
) : AcidifyEvent
