package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群解散事件
 * @property groupUin 群号
 * @property operatorUin 操作者 QQ 号
 * @property operatorUid 操作者 uid
 */
@JsExport
data class GroupDisbandEvent internal constructor(
    val groupUin: Long,
    val operatorUin: Long,
    val operatorUid: String
) : AcidifyEvent