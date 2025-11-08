package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * Bot 群成员数据
 * @property uin 群成员的 QQ 号
 * @property uid 群成员的 uid
 * @property nickname 群成员的昵称
 * @property card 群成员的群名片
 * @property specialTitle 群成员的群头衔
 * @property level 群成员的群等级（注意与 QQ 等级区分）
 * @property joinedAt 群成员的入群 Unix 时间戳（秒）
 * @property lastSpokeAt 群成员的最后发言 Unix 时间戳（秒）
 * @property mutedUntil 群成员的禁言截止 Unix 时间戳（秒）
 * @property role 群成员的角色（权限等级）
 */
@JsExport
data class BotGroupMemberData(
    val uin: Long,
    val uid: String,
    val nickname: String,
    val card: String,
    val specialTitle: String,
    val level: Int,
    val joinedAt: Long,
    val lastSpokeAt: Long,
    val mutedUntil: Long? = null,
    val role: GroupMemberRole,
)