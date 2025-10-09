package org.ntqqrev.acidify.entity

import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.struct.BotGroupMemberData
import org.ntqqrev.acidify.struct.GroupMemberRole

/**
 * 群成员实体
 * @property group 所属的群实体
 */
class BotGroupMember internal constructor(
    bot: Bot,
    data: BotGroupMemberData,
    val group: BotGroup,
) : AbstractEntity<BotGroupMemberData>(bot, data) {

    /**
     * 群成员的 QQ 号
     */
    val uin: Long
        get() = data.uin

    /**
     * 群成员的 uid
     */
    val uid: String
        get() = data.uid

    /**
     * 群成员的昵称
     */
    val nickname: String
        get() = data.nickname

    /**
     * 群成员的群名片
     */
    val card: String
        get() = data.card

    /**
     * 群成员的群头衔
     */
    val specialTitle: String
        get() = data.specialTitle

    /**
     * 群成员的群等级（注意与 QQ 等级区分）
     */
    val level: Int
        get() = data.level

    /**
     * 群成员的入群 Unix 时间戳（秒）
     */
    val joinedAt: Long
        get() = data.joinedAt

    /**
     * 群成员的最后发言 Unix 时间戳（秒）
     */
    val lastSpokeAt: Long
        get() = data.lastSpokeAt

    /**
     * 群成员的禁言截止 Unix 时间戳（秒）
     */
    val mutedUntil: Long?
        get() = data.mutedUntil

    /**
     * 群成员的角色（权限等级）
     */
    val role: GroupMemberRole
        get() = data.role

    override fun toString(): String {
        return "${card.ifEmpty { nickname }} ($uin)"
    }
}