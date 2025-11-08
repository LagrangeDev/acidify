package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 好友戳一戳事件
 * @property userUin 好友 QQ 号
 * @property userUid 好友 uid
 * @property isSelfSend 是否是自己发送的戳一戳
 * @property isSelfReceive 是否是自己接收的戳一戳
 * @property displayAction 戳一戳提示的动作文本
 * @property displaySuffix 戳一戳提示的后缀文本
 * @property displayActionImgUrl 戳一戳提示的动作图片 URL，用于取代动作提示文本
 */
@JsExport
class FriendNudgeEvent(
    val userUin: Long,
    val userUid: String,
    val isSelfSend: Boolean,
    val isSelfReceive: Boolean,
    val displayAction: String,
    val displaySuffix: String,
    val displayActionImgUrl: String
) : AcidifyEvent
