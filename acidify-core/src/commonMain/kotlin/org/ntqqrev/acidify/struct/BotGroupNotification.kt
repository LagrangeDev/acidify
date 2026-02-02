package org.ntqqrev.acidify.struct

import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.getUinByUid
import org.ntqqrev.acidify.internal.proto.oidb.GroupNotification
import kotlin.js.JsExport

/**
 * 群通知实体
 */
@JsExport
sealed class BotGroupNotification {
    /**
     * 用户入群请求
     * @property groupUin 群号
     * @property notificationSeq 通知序列号
     * @property isFiltered 请求是否被过滤（发起自风险账户）
     * @property initiatorUin 发起者 QQ 号
     * @property initiatorUid 发起者 uid
     * @property state 请求状态
     * @property operatorUin 处理请求的管理员 QQ 号
     * @property operatorUid 处理请求的管理员 uid
     * @property comment 入群请求附加信息
     */
    class JoinRequest internal constructor(
        val groupUin: Long,
        val notificationSeq: Long,
        val isFiltered: Boolean,
        val initiatorUin: Long,
        val initiatorUid: String,
        val state: RequestState,
        val operatorUin: Long?,
        val operatorUid: String?,
        val comment: String
    ) : BotGroupNotification()

    /**
     * 群管理员变更通知
     * @property groupUin 群号
     * @property notificationSeq 通知序列号
     * @property targetUserUin 被设置/取消用户 QQ 号
     * @property targetUserUid 被设置/取消用户 uid
     * @property isSet 是否被设置为管理员，`false` 表示被取消管理员
     * @property operatorUin 操作者（群主）QQ 号
     * @property operatorUid 操作者（群主）uid
     */
    class AdminChange internal constructor(
        val groupUin: Long,
        val notificationSeq: Long,
        val targetUserUin: Long,
        val targetUserUid: String,
        val isSet: Boolean,
        val operatorUin: Long,
        val operatorUid: String
    ) : BotGroupNotification()

    /**
     * 群成员被移除通知
     * @property groupUin 群号
     * @property notificationSeq 通知序列号
     * @property targetUserUin 被移除用户 QQ 号
     * @property targetUserUid 被移除用户 uid
     * @property operatorUin 移除用户的管理员 QQ 号
     * @property operatorUid 移除用户的管理员 uid
     */
    class Kick internal constructor(
        val groupUin: Long,
        val notificationSeq: Long,
        val targetUserUin: Long,
        val targetUserUid: String,
        val operatorUin: Long,
        val operatorUid: String
    ) : BotGroupNotification()

    /**
     * 群成员退群通知
     * @property groupUin 群号
     * @property notificationSeq 通知序列号
     * @property targetUserUin 退群用户 QQ 号
     * @property targetUserUid 退群用户 uid
     */
    class Quit internal constructor(
        val groupUin: Long,
        val notificationSeq: Long,
        val targetUserUin: Long,
        val targetUserUid: String
    ) : BotGroupNotification()

    /**
     * 群成员邀请他人入群请求
     * @property groupUin 群号
     * @property notificationSeq 通知序列号
     * @property initiatorUin 邀请者 QQ 号
     * @property initiatorUid 邀请者 uid
     * @property targetUserUin 被邀请用户 QQ 号
     * @property targetUserUid 被邀请用户 uid
     * @property state 请求状态
     * @property operatorUin 处理请求的管理员 QQ 号
     * @property operatorUid 处理请求的管理员 uid
     */
    class InvitedJoinRequest internal constructor(
        val groupUin: Long,
        val notificationSeq: Long,
        val initiatorUin: Long,
        val initiatorUid: String,
        val targetUserUin: Long,
        val targetUserUid: String,
        val state: RequestState,
        val operatorUin: Long?,
        val operatorUid: String?
    ) : BotGroupNotification()

    companion object {
        internal suspend fun Bot.parseNotification(
            raw: GroupNotification,
            isFiltered: Boolean
        ): BotGroupNotification? {
            val sequence = raw.sequence
            val notifyType = raw.notifyType
            val requestState = RequestState.from(raw.requestState)
            val group = raw.group
            val groupUin = group.groupUin
            val user1 = raw.user1
            val user1Uid = user1.uid
            val comment = raw.comment

            return when (notifyType) {
                1 -> {
                    val user1Uin = getUinByUid(user1Uid)
                    val user2 = raw.user2
                    val operatorUid = user2?.uid
                    val operatorUin = operatorUid?.let { getUinByUid(it) }
                    JoinRequest(
                        groupUin = groupUin,
                        notificationSeq = sequence,
                        isFiltered = isFiltered,
                        initiatorUin = user1Uin,
                        initiatorUid = user1Uid,
                        state = requestState,
                        operatorUin = operatorUin,
                        operatorUid = operatorUid,
                        comment = comment
                    )
                }

                3, 16 -> {
                    val user1Uin = getUinByUid(user1Uid)
                    val user2 = raw.user2 ?: return null
                    val user2Uid = user2.uid
                    val user2Uin = getUinByUid(user2Uid)
                    AdminChange(
                        groupUin = groupUin,
                        notificationSeq = sequence,
                        targetUserUin = user1Uin,
                        targetUserUid = user1Uid,
                        isSet = notifyType == 3,
                        operatorUin = user2Uin,
                        operatorUid = user2Uid
                    )
                }

                6 -> {
                    val user1Uin = getUinByUid(user1Uid)
                    val operator = raw.user2 ?: raw.user3 ?: return null
                    val operatorUid = operator.uid
                    val operatorUin = getUinByUid(operatorUid)
                    Kick(
                        groupUin = groupUin,
                        notificationSeq = sequence,
                        targetUserUin = user1Uin,
                        targetUserUid = user1Uid,
                        operatorUin = operatorUin,
                        operatorUid = operatorUid
                    )
                }

                13 -> {
                    val user1Uin = getUinByUid(user1Uid)
                    Quit(
                        groupUin = groupUin,
                        notificationSeq = sequence,
                        targetUserUin = user1Uin,
                        targetUserUid = user1Uid
                    )
                }

                22 -> {
                    val user1Uin = getUinByUid(user1Uid)
                    val user2 = raw.user2 ?: return null
                    val user2Uid = user2.uid
                    val user2Uin = getUinByUid(user2Uid)
                    val user3 = raw.user3
                    val operatorUid = user3?.uid
                    val operatorUin = operatorUid?.let { getUinByUid(it) }
                    InvitedJoinRequest(
                        groupUin = groupUin,
                        notificationSeq = sequence,
                        initiatorUin = user2Uin,
                        initiatorUid = user2Uid,
                        targetUserUin = user1Uin,
                        targetUserUid = user1Uid,
                        state = requestState,
                        operatorUin = operatorUin,
                        operatorUid = operatorUid
                    )
                }

                else -> null
            }
        }
    }
}