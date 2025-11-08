package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 群戳一戳事件
 * @property groupUin 群号
 * @property senderUin 发送者 QQ 号
 * @property senderUid 发送者 uid
 * @property receiverUin 接收者 QQ 号
 * @property receiverUid 接收者 uid
 * @property displayAction 戳一戳提示的动作文本
 * @property displaySuffix 戳一戳提示的后缀文本
 * @property displayActionImgUrl 戳一戳提示的动作图片 URL，用于取代动作提示文本
 */
@JsExport
class GroupNudgeEvent(
    val groupUin: Long,
    val senderUin: Long,
    val senderUid: String,
    val receiverUin: Long,
    val receiverUid: String,
    val displayAction: String,
    val displaySuffix: String,
    val displayActionImgUrl: String
) : AcidifyEvent