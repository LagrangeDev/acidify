package org.ntqqrev.acidify.struct

import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.getUinByUid
import org.ntqqrev.acidify.internal.proto.oidb.FilteredFriendRequestItem
import org.ntqqrev.acidify.internal.proto.oidb.FriendRequestItem
import kotlin.js.JsExport

/**
 * 好友请求实体
 *
 * @property time 请求发起时间（Unix 时间戳，秒）
 * @property initiatorUin 请求发起者 QQ 号
 * @property initiatorUid 请求发起者 uid
 * @property targetUserUin 目标用户 QQ 号
 * @property targetUserUid 目标用户 uid
 * @property state 请求状态
 * @property comment 申请附加信息
 * @property via 申请来源
 * @property isFiltered 请求是否被过滤（发起自风险账户）
 */
@JsExport
class BotFriendRequest internal constructor(
    val time: Long,
    val initiatorUin: Long,
    val initiatorUid: String,
    val targetUserUin: Long,
    val targetUserUid: String,
    val state: RequestState,
    val comment: String,
    val via: String,
    val isFiltered: Boolean
) {
    companion object {
        internal suspend fun Bot.parseFriendRequest(
            raw: FriendRequestItem
        ) = BotFriendRequest(
            time = raw.timestamp,
            initiatorUin = getUinByUid(raw.sourceUid),
            initiatorUid = raw.sourceUid,
            targetUserUin = getUinByUid(raw.targetUid),
            targetUserUid = raw.targetUid,
            state = when (raw.state) {
                1 -> RequestState.PENDING
                3 -> RequestState.ACCEPTED
                7 -> RequestState.REJECTED
                else -> RequestState.DEFAULT
            },
            comment = raw.comment,
            via = raw.source,
            isFiltered = false
        )

        internal suspend fun Bot.parseFilteredFriendRequest(
            raw: FilteredFriendRequestItem
        ) = BotFriendRequest(
            time = raw.timestamp,
            initiatorUin = getUinByUid(raw.sourceUid),
            initiatorUid = raw.sourceUid,
            targetUserUin = uin,
            targetUserUid = uid,
            state = RequestState.PENDING,
            comment = raw.comment,
            via = raw.source,
            isFiltered = true
        )
    }
}