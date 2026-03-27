package org.ntqqrev.acidify.event.internal

import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.event.*
import org.ntqqrev.acidify.internal.proto.message.CommonMessage
import org.ntqqrev.acidify.internal.proto.message.PushMsg
import org.ntqqrev.acidify.internal.proto.message.PushMsgType
import org.ntqqrev.acidify.internal.proto.message.RoutingHead
import org.ntqqrev.acidify.internal.proto.message.extra.*
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.message.BotIncomingSegment
import org.ntqqrev.acidify.message.MessageScene
import org.ntqqrev.acidify.message.internal.parseMessage
import org.ntqqrev.acidify.struct.BotGroupNotification
import org.ntqqrev.acidify.struct.RequestState
import kotlin.time.Clock

@Suppress("duplicatedCode")
internal object MsgPushSignal : AbstractSignal("trpc.msg.olpush.OlPushService.MsgPush") {
    override suspend fun parse(bot: AbstractBot, payload: ByteArray): List<AcidifyEvent> {
        val commonMsg = payload.pbDecode<PushMsg>().message
        val contentHead = commonMsg.contentHead
        val routingHead = commonMsg.routingHead
        val msgBody = commonMsg.messageBody
        val msgContent = msgBody.msgContent
        val pushMsgType = PushMsgType.from(contentHead.type)

        when (pushMsgType) {
            PushMsgType.FriendMessage,
            PushMsgType.FriendRecordMessage,
            PushMsgType.FriendFileMessage,
            PushMsgType.GroupMessage -> {
                return parseMessage(bot, commonMsg)
            }

            else -> {}
        }

        if (msgContent.isEmpty()) {
            return emptyList()
        }

        return when (pushMsgType) {
            PushMsgType.GroupJoinRequest -> parseGroupJoinRequest(bot, msgContent)
            PushMsgType.GroupInvitedJoinRequest -> parseGroupInvitedJoinRequest(bot, msgContent)
            PushMsgType.GroupAdminChange -> parseGroupAdminChange(bot, msgContent)
            PushMsgType.GroupMemberIncrease -> parseGroupMemberIncrease(bot, msgContent)
            PushMsgType.GroupMemberDecrease -> parseGroupMemberDecrease(bot, msgContent)
            PushMsgType.Event0x210 -> parseEvent0x210(bot, routingHead, contentHead.subType, msgContent)
            PushMsgType.Event0x2DC -> parseEvent0x2DC(bot, contentHead.subType, msgContent)

            else -> emptyList()
        }
    }

    private fun parseMessage(bot: AbstractBot, commonMsg: CommonMessage): List<AcidifyEvent> {
        val msg = bot.parseMessage(commonMsg) ?: return emptyList()

        // 根据 extraInfo 刷新群成员信息
        if (msg.scene == MessageScene.GROUP && msg.extraInfo != null) {
            bot.launch {
                val member = bot.getGroup(msg.peerUin)?.getMember(msg.senderUin)
                member?.updateBinding(
                    member.data.copy(
                        nickname = msg.extraInfo.nick,
                        card = msg.extraInfo.groupCard,
                        specialTitle = msg.extraInfo.specialTitle
                    )
                )
            }
        }

        val mutList = mutableListOf<AcidifyEvent>(MessageReceiveEvent(msg))

        msg.segments.filterIsInstance<BotIncomingSegment.LightApp>()
            .firstOrNull()
            ?.takeIf { it.appName == "com.tencent.qun.invite" || it.appName == "com.tencent.tuwen.lua" }
            ?.let {
                Json.parseToJsonElement(it.jsonPayload)
                    .jsonObject
                    .takeIf { obj -> obj["bizsrc"]?.jsonPrimitive?.content == "qun.invite" }
                    ?.get("meta")
                    ?.jsonObject["news"]
                    ?.jsonObject["jumpUrl"]
                    ?.jsonPrimitive?.content
            }?.let {
                parseUrl(it)
            }?.takeIf {
                it.parameters["groupcode"] != null && it.parameters["msgseq"] != null
            }?.let {
                mutList += GroupInvitationEvent(
                    groupUin = it.parameters["groupcode"]!!.toLong(),
                    invitationSeq = it.parameters["msgseq"]!!.toLong(),
                    initiatorUin = msg.senderUin,
                    initiatorUid = msg.senderUid
                )
            }

        msg.segments.filterIsInstance<BotIncomingSegment.File>()
            .firstOrNull()
            ?.let {
                if (msg.scene == MessageScene.FRIEND) {
                    mutList += FriendFileUploadEvent(
                        userUin = msg.senderUin,
                        userUid = msg.senderUid,
                        isSelf = msg.senderUin == bot.uin,
                        fileName = it.fileName,
                        fileSize = it.fileSize,
                        fileId = it.fileId,
                        fileHash = it.fileHash!!
                    )
                } else if (msg.scene == MessageScene.GROUP) {
                    mutList += GroupFileUploadEvent(
                        groupUin = msg.peerUin,
                        userUin = msg.senderUin,
                        userUid = msg.senderUid,
                        fileName = it.fileName,
                        fileSize = it.fileSize,
                        fileId = it.fileId
                    )
                }
            }

        return mutList
    }

    private suspend fun parseGroupJoinRequest(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val content = msgContent.pbDecode<GroupJoinRequest>()
        val groupUin = content.groupUin
        val memberUid = content.memberUid
        val memberUin = bot.getUinByUid(memberUid)

        val recentNotifications =
            bot.getGroupNotifications(isFiltered = false, count = 30).first +
                    bot.getGroupNotifications(isFiltered = true, count = 10).first

        return recentNotifications
            .filterIsInstance<BotGroupNotification.JoinRequest>()
            .find {
                it.groupUin == groupUin &&
                        it.initiatorUin == memberUin &&
                        it.state == RequestState.PENDING
            }
            ?.let {
                listOf(
                    GroupJoinRequestEvent(
                        groupUin = groupUin,
                        notificationSeq = it.notificationSeq,
                        isFiltered = it.isFiltered,
                        initiatorUin = memberUin,
                        initiatorUid = memberUid,
                        comment = it.comment
                    )
                )
            }
            ?: emptyList()
    }

    private suspend fun parseGroupInvitedJoinRequest(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val content = msgContent.pbDecode<GroupInvitedJoinRequest>()
        if (content.command == 87) {
            val info = content.info ?: return emptyList()
            val inner = info.inner ?: return emptyList()
            val groupUin = inner.groupUin
            val targetUid = inner.targetUid
            val invitorUid = inner.invitorUid
            val targetUin = bot.getUinByUid(targetUid)
            val invitorUin = bot.getUinByUid(invitorUid)

            val recentNotifications =
                bot.getGroupNotifications(isFiltered = false, count = 30).first +
                        bot.getGroupNotifications(isFiltered = true, count = 10).first

            return recentNotifications
                .filterIsInstance<BotGroupNotification.InvitedJoinRequest>()
                .find {
                    it.groupUin == groupUin &&
                            it.initiatorUin == invitorUin &&
                            it.targetUserUin == targetUin &&
                            it.state == RequestState.PENDING
                }
                ?.let {
                    listOf(
                        GroupInvitedJoinRequestEvent(
                            groupUin = groupUin,
                            notificationSeq = it.notificationSeq,
                            initiatorUin = invitorUin,
                            initiatorUid = invitorUid,
                            targetUserUin = targetUin,
                            targetUserUid = targetUid
                        )
                    )
                }
                ?: emptyList()
        } else {
            return emptyList()
        }
    }

    private suspend fun parseGroupAdminChange(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val content = msgContent.pbDecode<GroupAdminChange>()
        val groupUin = content.groupUin
        val group = bot.getGroup(groupUin) ?: return emptyList()
        group.getMembers() // ensure members are loaded, thus owner info is available
        val body = content.body ?: return emptyList()
        val (targetUid, isSet) = if (body.set != null) {
            body.set.targetUid to true
        } else if (body.unset != null) {
            body.unset.targetUid to false
        } else {
            return emptyList()
        }
        val targetUin = bot.getUinByUid(targetUid)
        return listOf(
            GroupAdminChangeEvent(
                groupUin = groupUin,
                userUin = targetUin,
                userUid = targetUid,
                operatorUin = group.owner.uin,
                operatorUid = group.owner.uid,
                isSet = isSet,
            )
        )
    }

    private suspend fun parseGroupMemberIncrease(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val content = msgContent.pbDecode<GroupMemberChange>()
        val groupUin = content.groupUin
        val memberUid = content.memberUid
        val memberUin = bot.getUinByUid(memberUid)
        val operatorInfoBytes = content.operatorInfo ?: return emptyList()
        val operatorUid = operatorInfoBytes.decodeToString()
        val operatorUin = bot.getUinByUid(operatorUid)

        return when (content.type) {
            130 -> listOf(
                GroupMemberIncreaseEvent(
                    groupUin = groupUin,
                    userUin = memberUin,
                    userUid = memberUid,
                    operatorUin = operatorUin,
                    operatorUid = operatorUid,
                    invitorUin = null,
                    invitorUid = null
                )
            )

            131 -> listOf(
                GroupMemberIncreaseEvent(
                    groupUin = groupUin,
                    userUin = memberUin,
                    userUid = memberUid,
                    operatorUin = null,
                    operatorUid = null,
                    invitorUin = operatorUin,
                    invitorUid = operatorUid
                )
            )

            else -> emptyList()
        }
    }

    private suspend fun parseGroupMemberDecrease(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val content = msgContent.pbDecode<GroupMemberChange>()
        val groupUin = content.groupUin
        val memberUid = content.memberUid
        val memberUin = bot.getUinByUid(memberUid)
        val operatorUid = content.operatorInfo
            ?.pbDecode<GroupMemberChange.OperatorInfo>()
            ?.body
            ?.uid
        val operatorUin = operatorUid?.let { bot.getUinByUid(it) }

        return listOf(
            GroupMemberDecreaseEvent(
                groupUin = groupUin,
                userUin = memberUin,
                userUid = memberUid,
                operatorUin = operatorUin,
                operatorUid = operatorUid
            )
        )
    }

    private fun parseFriendRequest(routingHead: RoutingHead, msgContent: ByteArray): List<AcidifyEvent> {
        val content = msgContent.pbDecode<FriendRequest>()
        val body = content.body ?: return emptyList()
        val fromUid = body.fromUid
        val fromUin = routingHead.fromUin
        val comment = body.message
        val via = body.via ?: msgContent.pbDecode<FriendRequestExtractVia>()
            .body?.via ?: ""

        return listOf(
            FriendRequestEvent(
                initiatorUin = fromUin,
                initiatorUid = fromUid,
                comment = comment,
                via = via
            )
        )
    }

    private suspend fun parseEvent0x210(
        bot: AbstractBot,
        routingHead: RoutingHead,
        subType: Int,
        msgContent: ByteArray
    ): List<AcidifyEvent> = when (subType) {
        35 -> parseFriendRequest(routingHead, msgContent)
        290 -> parseFriendNudge(bot, routingHead, msgContent)
        138, 139 -> parseFriendRecall(bot, subType, msgContent)
        39 -> parseFriendDeleteOrPinChanged(bot, msgContent)
        else -> emptyList()
    }

    private suspend fun parseFriendNudge(
        bot: AbstractBot,
        routingHead: RoutingHead,
        msgContent: ByteArray
    ): List<AcidifyEvent> {
        val content = msgContent.pbDecode<GeneralGrayTip>()
        if (content.bizType != 12) return emptyList()
        val fromUin = routingHead.fromUin
        val uin1 = content.templateParams["uin_str1"]?.toLongOrNull() ?: return emptyList()
        val uin2 = content.templateParams["uin_str2"]?.toLongOrNull() ?: return emptyList()
        val action = content.templateParams["action_str"]
            ?: content.templateParams["alt_str1"] ?: ""
        val actionImgUrl = content.templateParams["action_img_url"] ?: ""
        val suffix = content.templateParams["suffix_str"] ?: ""

        return listOf(
            FriendNudgeEvent(
                userUin = fromUin,
                userUid = bot.getUidByUin(fromUin),
                isSelfSend = uin1 == bot.uin,
                isSelfReceive = uin2 == bot.uin,
                displayAction = action,
                displaySuffix = suffix,
                displayActionImgUrl = actionImgUrl
            )
        )
    }

    private suspend fun parseFriendRecall(
        bot: AbstractBot,
        subType: Int,
        msgContent: ByteArray
    ): List<AcidifyEvent> {
        val content = msgContent.pbDecode<FriendRecall>()
        val body = content.body ?: return emptyList()
        val fromUid = body.fromUid
        val toUid = body.toUid
        val sequence = body.sequence.toLong()
        val displaySuffix = body.tipInfo?.tip ?: ""
        val fromUin = bot.getUinByUid(fromUid)
        val toUin = bot.getUinByUid(toUid)

        return listOf(
            MessageRecallEvent(
                scene = MessageScene.FRIEND,
                peerUin = if (subType == 0x122) toUin else fromUin,
                messageSeq = sequence,
                senderUin = fromUin,
                senderUid = fromUid,
                operatorUin = fromUin,
                operatorUid = fromUid,
                displaySuffix = displaySuffix
            )
        )
    }

    private suspend fun parseFriendDeleteOrPinChanged(
        bot: AbstractBot,
        msgContent: ByteArray
    ): List<AcidifyEvent> {
        val content = msgContent.pbDecode<FriendDeleteOrPinChanged>()
        val body = content.body
        val pinChanged = body.pinChanged ?: return emptyList()
        val pinBody = pinChanged.body
        val uid = pinBody.uid
        val groupUin = pinBody.groupUin
        val isPin = pinBody.info.timestamp.isNotEmpty()
        val (scene, targetUin) = if (groupUin != null) {
            MessageScene.GROUP to groupUin
        } else {
            val uin = runCatching { bot.getUinByUid(uid) }.getOrNull() ?: return emptyList()
            MessageScene.FRIEND to uin
        }

        return listOf(
            PinChangedEvent(
                scene = scene,
                peerUin = targetUin,
                isPinned = isPin
            )
        )
    }

    private suspend fun parseEvent0x2DC(
        bot: AbstractBot,
        subType: Int,
        msgContent: ByteArray
    ): List<AcidifyEvent> = when (subType) {
        12 -> parseGroupMute(bot, msgContent)
        20 -> parseGroupGrayTip(bot, msgContent)
        21 -> parseGroupEssenceMessageChange(msgContent)
        17 -> parseGroupRecall(bot, msgContent)
        16 -> parseGroupSubType16(bot, msgContent)
        else -> emptyList()
    }

    private suspend fun parseGroupMute(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val content = msgContent.pbDecode<GroupMute>()
        val groupUin = content.groupUin
        val operatorUid = content.operatorUid
        val operatorUin = bot.getUinByUid(operatorUid)
        val info = content.info ?: return emptyList()
        val state = info.state ?: return emptyList()
        val targetUid = state.targetUid
        val duration = state.duration

        return if (targetUid != null) {
            val targetUin = bot.getUinByUid(targetUid)
            val member = bot.getGroup(groupUin)?.getMember(targetUin)
            member?.updateBinding(
                member.data.copy(
                    mutedUntil = Clock.System.now().epochSeconds
                )
            )
            listOf(
                GroupMuteEvent(
                    groupUin = groupUin,
                    userUin = targetUin,
                    userUid = targetUid,
                    operatorUin = operatorUin,
                    operatorUid = operatorUid,
                    duration = duration
                )
            )
        } else {
            listOf(
                GroupWholeMuteEvent(
                    groupUin = groupUin,
                    operatorUin = operatorUin,
                    operatorUid = operatorUid,
                    isMute = duration != 0
                )
            )
        }
    }

    private suspend fun parseGroupGrayTip(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val wrapper = GroupGeneral0x2DC(msgContent)
        val body = wrapper.body
        val content = body.generalGrayTip ?: return emptyList()

        if (content.bizType != 12) return emptyList()

        val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
        val uin1 = content.templateParams["uin_str1"]?.toLongOrNull() ?: return emptyList()
        val uin2 = content.templateParams["uin_str2"]?.toLongOrNull() ?: return emptyList()
        val action = content.templateParams["action_str"]
            ?: content.templateParams["alt_str1"] ?: ""
        val actionImgUrl = content.templateParams["action_img_url"] ?: ""
        val suffix = content.templateParams["suffix_str"] ?: ""

        return listOf(
            GroupNudgeEvent(
                groupUin = groupUin,
                senderUin = uin1,
                senderUid = bot.getUidByUin(uin1, groupUin),
                receiverUin = uin2,
                receiverUid = bot.getUidByUin(uin2, groupUin),
                displayAction = action,
                displaySuffix = suffix,
                displayActionImgUrl = actionImgUrl
            )
        )
    }

    private fun parseGroupEssenceMessageChange(msgContent: ByteArray): List<AcidifyEvent> {
        val wrapper = GroupGeneral0x2DC(msgContent)
        val body = wrapper.body
        val content = body.essenceMessageChange ?: return emptyList()
        val groupUin = content.groupUin
        val msgSeq = content.msgSequence.toLong()
        val operatorUin = content.operatorUin
        val isSet = content.setFlag == 1

        return listOf(
            GroupEssenceMessageChangeEvent(
                groupUin = groupUin,
                messageSeq = msgSeq,
                operatorUin = operatorUin,
                isSet = isSet,
            )
        )
    }

    private suspend fun parseGroupRecall(bot: AbstractBot, msgContent: ByteArray): List<AcidifyEvent> {
        val wrapper = GroupGeneral0x2DC(msgContent)
        val body = wrapper.body
        val content = body.recall ?: return emptyList()
        val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
        val operatorUid = content.operatorUid
        val operatorUin = bot.getUinByUid(operatorUid)
        val displaySuffix = content.tipInfo?.tip ?: ""

        return content.recallMessages.map { recall ->
            bot.async {
                val authorUid = recall.authorUid
                val authorUin = bot.getUinByUid(authorUid)
                MessageRecallEvent(
                    scene = MessageScene.GROUP,
                    peerUin = groupUin,
                    messageSeq = recall.sequence.toLong(),
                    senderUin = authorUin,
                    senderUid = authorUid,
                    operatorUin = operatorUin,
                    operatorUid = operatorUid,
                    displaySuffix = displaySuffix
                )
            }
        }.awaitAll()
    }

    private suspend fun parseGroupSubType16(
        bot: AbstractBot,
        msgContent: ByteArray
    ): List<AcidifyEvent> {
        val wrapper = GroupGeneral0x2DC(msgContent)
        val body = wrapper.body
        return when (body.field13) {
            35 -> parseGroupReaction(bot, wrapper, body)
            12 -> parseGroupNameChange(bot, wrapper, body)
            else -> emptyList()
        }
    }

    private suspend fun parseGroupReaction(
        bot: AbstractBot,
        wrapper: GroupGeneral0x2DC,
        body: GroupGeneral0x2DC.Body
    ): List<AcidifyEvent> {
        val content = body.reaction ?: return emptyList()
        val data = content.data ?: return emptyList()
        val dataMiddle = data.data ?: return emptyList()
        val target = dataMiddle.target ?: return emptyList()
        val dataInner = dataMiddle.data ?: return emptyList()

        val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
        val msgSeq = target.sequence.toLong()
        val operatorUid = dataInner.operatorUid
        val operatorUin = bot.getUinByUid(operatorUid)
        val faceId = dataInner.code
        val reactionType = dataInner.reactionType
        val isAdd = dataInner.type == 1

        return listOf(
            GroupMessageReactionEvent(
                groupUin = groupUin,
                userUin = operatorUin,
                userUid = operatorUid,
                messageSeq = msgSeq,
                faceId = faceId,
                type = reactionType,
                isAdd = isAdd
            )
        )
    }

    private suspend fun parseGroupNameChange(
        bot: AbstractBot,
        wrapper: GroupGeneral0x2DC,
        body: GroupGeneral0x2DC.Body
    ): List<AcidifyEvent> {
        val eventParam = body.eventParam ?: return emptyList()
        val content = eventParam.pbDecode<GroupNameChange>()
        val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
        val newName = content.name
        val operatorUid = body.operatorUid ?: return emptyList()
        val operatorUin = bot.getUinByUid(operatorUid)

        return listOf(
            GroupNameChangeEvent(
                groupUin = groupUin,
                newGroupName = newName,
                operatorUin = operatorUin,
                operatorUid = operatorUid
            )
        )
    }
}
