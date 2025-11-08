package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群成员减少事件
 * @property groupUin 群号
 * @property userUin 发生变更的用户 QQ 号
 * @property userUid 发生变更的用户 uid
 * @property operatorUin 管理员 QQ 号，如果是管理员踢出
 * @property operatorUid 管理员 uid，如果是管理员踢出
 */
@JsExport
class GroupMemberDecreaseEvent(
    val groupUin: Long,
    val userUin: Long,
    val userUid: String,
    val operatorUin: Long?,
    val operatorUid: String?
) : AcidifyEvent
