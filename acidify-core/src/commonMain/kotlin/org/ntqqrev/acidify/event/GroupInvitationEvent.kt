package org.ntqqrev.acidify.event

import kotlin.js.JsExport

/**
 * 他人邀请自身入群事件
 * @property groupUin 群号
 * @property invitationSeq 邀请序列号
 * @property initiatorUin 邀请者 QQ 号
 * @property initiatorUid 邀请者 uid
 */
@JsExport
class GroupInvitationEvent(
    val groupUin: Long,
    val invitationSeq: Long,
    val initiatorUin: Long,
    val initiatorUid: String
) : AcidifyEvent
