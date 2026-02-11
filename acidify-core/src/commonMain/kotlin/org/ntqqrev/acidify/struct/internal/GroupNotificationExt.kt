package org.ntqqrev.acidify.struct.internal

import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.getUinByUid
import org.ntqqrev.acidify.internal.proto.oidb.GroupNotification
import org.ntqqrev.acidify.struct.BotGroupNotification
import org.ntqqrev.acidify.struct.BotGroupNotification.*
import org.ntqqrev.acidify.struct.RequestState

internal suspend fun AbstractBot.parseNotification(
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