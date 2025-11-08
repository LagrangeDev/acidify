package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群名称变更事件
 * @property groupUin 群号
 * @property newGroupName 新的群名称
 * @property operatorUin 操作者 QQ 号
 * @property operatorUid 操作者 uid
 */
@JsExport
class GroupNameChangeEvent(
    val groupUin: Long,
    val newGroupName: String,
    val operatorUin: Long,
    val operatorUid: String
) : AcidifyEvent