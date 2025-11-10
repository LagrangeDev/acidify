package org.ntqqrev.acidify.entity

import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.common.CacheUtility
import org.ntqqrev.acidify.struct.BotGroupData
import org.ntqqrev.acidify.struct.BotGroupMemberData
import kotlin.js.JsExport

/**
 * 群实体
 */
@JsExport
class BotGroup internal constructor(
    bot: Bot,
    data: BotGroupData,
) : AbstractEntity<BotGroupData>(bot, data) {

    /**
     * 群成员缓存服务
     */
    private val memberCache = CacheUtility<Long, BotGroupMember, BotGroupMemberData>(
        bot = bot,
        updateCache = { bot -> bot.fetchGroupMembers(uin).associateBy { it.uin } },
        entityFactory = { bot, data -> BotGroupMember(bot, data, this) }
    )

    /**
     * 群的 uin（群号）
     */
    val uin: Long
        get() = data.uin

    /**
     * 群名称
     */
    val name: String
        get() = data.name

    /**
     * 群成员数量
     */
    val memberCount: Int
        get() = data.memberCount

    /**
     * 群容量
     */
    val capacity: Int
        get() = data.capacity

    /**
     * 获取所有群成员
     *
     * @param forceUpdate 是否强制更新缓存
     * @return 所有群成员的列表
     */
    @JsExport.Ignore
    suspend fun getMembers(forceUpdate: Boolean = false): List<BotGroupMember> {
        return memberCache.getAll(forceUpdate)
    }

    /**
     * 根据 uin 获取群成员
     *
     * @param uin 群成员的 QQ 号
     * @param forceUpdate 是否强制更新缓存
     * @return 群成员实体，如果不存在则返回 null
     */
    @JsExport.Ignore
    suspend fun getMember(uin: Long, forceUpdate: Boolean = false): BotGroupMember? {
        return memberCache.get(uin, forceUpdate)
    }

    /**
     * 更新群成员缓存
     */
    @JsExport.Ignore
    suspend fun updateMemberCache() {
        memberCache.update()
    }

    override fun toString(): String {
        return "$name ($uin)"
    }
}