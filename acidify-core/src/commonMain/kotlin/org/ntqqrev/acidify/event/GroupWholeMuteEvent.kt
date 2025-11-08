package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群全体禁言事件
 * @property groupUin 群号
 * @property operatorUin 操作者 QQ 号
 * @property operatorUid 操作者 uid
 * @property isMute 是否全员禁言，`false` 表示取消全员禁言
 */
@JsExport
class GroupWholeMuteEvent(
    val groupUin: Long,
    val operatorUin: Long,
    val operatorUid: String,
    val isMute: Boolean
) : AcidifyEvent