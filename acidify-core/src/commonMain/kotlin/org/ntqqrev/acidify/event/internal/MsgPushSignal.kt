package org.ntqqrev.acidify.event.internal

import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.event.*
import org.ntqqrev.acidify.internal.proto.message.PushMsg
import org.ntqqrev.acidify.internal.proto.message.PushMsgType
import org.ntqqrev.acidify.internal.proto.message.extra.*
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.message.BotIncomingSegment
import org.ntqqrev.acidify.message.MessageScene
import org.ntqqrev.acidify.message.internal.MessageParsingContext.Companion.parseMessage
import org.ntqqrev.acidify.struct.BotGroupNotification
import org.ntqqrev.acidify.struct.RequestState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal object MsgPushSignal : AbstractSignal("trpc.msg.olpush.OlPushService.MsgPush") {
    @OptIn(ExperimentalTime::class)
    @Suppress("duplicatedCode")
    override suspend fun parse(bot: Bot, payload: ByteArray): List<AcidifyEvent> {
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
                val msg = bot.parseMessage(commonMsg) ?: return listOf()

                // 根据 extraInfo 刷新群成员信息
                if (msg.scene == MessageScene.GROUP && msg.extraInfo != null) {
                    val member = bot.getGroup(msg.peerUin)?.getMember(msg.senderUin)
                    member?.updateBinding(
                        member.data.copy(
                            nickname = msg.extraInfo!!.nick,
                            card = msg.extraInfo!!.groupCard,
                            specialTitle = msg.extraInfo!!.specialTitle
                        )
                    )
                }

                val mutList = mutableListOf<AcidifyEvent>(MessageReceiveEvent(msg))

                msg.segments.filterIsInstance<BotIncomingSegment.LightApp>()
                    .firstOrNull()
                    ?.takeIf { it.appName == "com.tencent.qun.invite" || it.appName == "com.tencent.tuwen.lua" }
                    ?.let {
                        Json.decodeFromString<JsonElement>(it.jsonPayload)
                            .jsonObject
                            .takeIf { it["bizsrc"]?.jsonPrimitive?.content == "qun.invite" }
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

            else -> {}
        }

        if (msgContent.isEmpty()) {
            return listOf()
        }

        return when (pushMsgType) {
            PushMsgType.GroupJoinRequest -> {
                val content = msgContent.pbDecode<GroupJoinRequest>()
                val groupUin = content.groupUin
                val memberUid = content.memberUid
                val memberUin = bot.getUinByUid(memberUid)

                val recentNotifications =
                    bot.getGroupNotifications(isFiltered = false, count = 30).first +
                            bot.getGroupNotifications(isFiltered = true, count = 10).first

                recentNotifications
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
                    ?: listOf()
            }

            PushMsgType.GroupInvitedJoinRequest -> {
                val content = msgContent.pbDecode<GroupInvitedJoinRequest>()
                if (content.command == 87) {
                    val info = content.info ?: return listOf()
                    val inner = info.inner ?: return listOf()
                    val groupUin = inner.groupUin
                    val targetUid = inner.targetUid
                    val invitorUid = inner.invitorUid
                    val targetUin = bot.getUinByUid(targetUid)
                    val invitorUin = bot.getUinByUid(invitorUid)

                    val recentNotifications =
                        bot.getGroupNotifications(isFiltered = false, count = 30).first +
                                bot.getGroupNotifications(isFiltered = true, count = 10).first

                    recentNotifications
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
                        ?: listOf()
                } else {
                    listOf()
                }
            }

            PushMsgType.GroupAdminChange -> {
                val content = msgContent.pbDecode<GroupAdminChange>()
                val groupUin = content.groupUin
                val group = bot.getGroup(groupUin) ?: return listOf()
                group.getMembers() // ensure members are loaded, thus owner info is available
                val body = content.body ?: return listOf()
                val (targetUid, isSet) = if (body.set != null) {
                    body.set.targetUid to true
                } else if (body.unset != null) {
                    body.unset.targetUid to false
                } else {
                    return listOf()
                }
                val targetUin = bot.getUinByUid(targetUid)
                listOf(
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

            PushMsgType.GroupMemberIncrease -> {
                val content = msgContent.pbDecode<GroupMemberChange>()
                val groupUin = content.groupUin
                val memberUid = content.memberUid
                val memberUin = bot.getUinByUid(memberUid)
                val operatorInfoBytes = content.operatorInfo ?: return listOf()
                val operatorUid = operatorInfoBytes.decodeToString()
                val operatorUin = bot.getUinByUid(operatorUid)

                when (content.type) {
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

                    else -> listOf()
                }
            }

            PushMsgType.GroupMemberDecrease -> {
                val content = msgContent.pbDecode<GroupMemberChange>()
                val groupUin = content.groupUin
                val memberUid = content.memberUid
                val memberUin = bot.getUinByUid(memberUid)
                val operatorUid = content.operatorInfo
                    ?.pbDecode<GroupMemberChange.OperatorInfo>()
                    ?.body
                    ?.uid
                val operatorUin = operatorUid?.let { bot.getUinByUid(it) }

                listOf(
                    GroupMemberDecreaseEvent(
                        groupUin = groupUin,
                        userUin = memberUin,
                        userUid = memberUid,
                        operatorUin = operatorUin,
                        operatorUid = operatorUid
                    )
                )
            }

            PushMsgType.Event0x210 -> {
                when (val subType = contentHead.subType) {
                    35 -> { // FriendRequest
                        val content = msgContent.pbDecode<FriendRequest>()
                        val body = content.body ?: return listOf()
                        val fromUid = body.fromUid
                        val fromUin = routingHead.fromUin
                        val comment = body.message
                        val via = body.via ?: msgContent.pbDecode<FriendRequestExtractVia>()
                            .body?.via ?: ""

                        listOf(
                            FriendRequestEvent(
                                initiatorUin = fromUin,
                                initiatorUid = fromUid,
                                comment = comment,
                                via = via
                            )
                        )
                    }

                    290 -> { // FriendGrayTip (FriendNudge)
                        val content = msgContent.pbDecode<GeneralGrayTip>()
                        if (content.bizType == 12) {
                            val fromUin = routingHead.fromUin
                            val uin1 = content.templateParams["uin_str1"]?.toLongOrNull() ?: return listOf()
                            val uin2 = content.templateParams["uin_str2"]?.toLongOrNull() ?: return listOf()
                            val action = content.templateParams["action_str"]
                                ?: content.templateParams["alt_str1"] ?: ""
                            val actionImgUrl = content.templateParams["action_img_url"] ?: ""
                            val suffix = content.templateParams["suffix_str"] ?: ""

                            listOf(
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
                        } else {
                            listOf()
                        }
                    }

                    138, 139 -> { // FriendRecall, FriendSelfRecall
                        val content = msgContent.pbDecode<FriendRecall>()
                        val body = content.body ?: return listOf()
                        val fromUid = body.fromUid
                        val toUid = body.toUid
                        val sequence = body.sequence.toLong()
                        val displaySuffix = body.tipInfo?.tip ?: ""
                        val fromUin = bot.getUinByUid(fromUid)
                        val toUin = bot.getUinByUid(toUid)

                        listOf(
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

                    39 -> { // FriendDeleteOrPinChanged
                        val content = msgContent.pbDecode<FriendDeleteOrPinChanged>()
                        val body = content.body
                        val pinChanged = body.pinChanged ?: return listOf()
                        val pinBody = pinChanged.body
                        val uid = pinBody.uid
                        val groupUin = pinBody.groupUin
                        val isPin = pinBody.info.timestamp.isNotEmpty()
                        val (scene, targetUin) = if (groupUin != null) {
                            MessageScene.GROUP to groupUin
                        } else {
                            val uin = runCatching { bot.getUinByUid(uid) }.getOrNull() ?: return listOf()
                            MessageScene.FRIEND to uin
                        }

                        listOf(
                            PinChangedEvent(
                                scene = scene,
                                peerUin = targetUin,
                                isPinned = isPin
                            )
                        )
                    }

                    else -> listOf()
                }
            }

            PushMsgType.Event0x2DC -> {
                val subType = contentHead.subType
                when (subType) {
                    12 -> { // GroupMute
                        val content = msgContent.pbDecode<GroupMute>()
                        val groupUin = content.groupUin
                        val operatorUid = content.operatorUid
                        val operatorUin = bot.getUinByUid(operatorUid)
                        val info = content.info ?: return listOf()
                        val state = info.state ?: return listOf()
                        val targetUid = state.targetUid
                        val duration = state.duration

                        if (targetUid != null) {
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

                    20 -> { // GroupGrayTip (may contain GroupNudge)
                        val wrapper = GroupGeneral0x2DC(msgContent)
                        val body = wrapper.body
                        val content = body.generalGrayTip ?: return listOf()

                        if (content.bizType == 12) {
                            val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
                            val uin1 = content.templateParams["uin_str1"]?.toLongOrNull() ?: return listOf()
                            val uin2 = content.templateParams["uin_str2"]?.toLongOrNull() ?: return listOf()
                            val action = content.templateParams["action_str"]
                                ?: content.templateParams["alt_str1"] ?: ""
                            val actionImgUrl = content.templateParams["action_img_url"] ?: ""
                            val suffix = content.templateParams["suffix_str"] ?: ""

                            listOf(
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
                        } else {
                            listOf()
                        }
                    }

                    21 -> { // GroupEssenceMessageChange
                        val wrapper = GroupGeneral0x2DC(msgContent)
                        val body = wrapper.body
                        val content = body.essenceMessageChange ?: return listOf()
                        val groupUin = content.groupUin
                        val msgSeq = content.msgSequence.toLong()
                        val operatorUin = content.operatorUin
                        val isSet = content.setFlag == 1

                        listOf(
                            GroupEssenceMessageChangeEvent(
                                groupUin = groupUin,
                                messageSeq = msgSeq,
                                operatorUin = operatorUin,
                                isSet = isSet,
                            )
                        )
                    }

                    17 -> { // GroupRecall
                        val wrapper = GroupGeneral0x2DC(msgContent)
                        val body = wrapper.body
                        val content = body.recall ?: return listOf()
                        val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
                        val operatorUid = content.operatorUid
                        val operatorUin = bot.getUinByUid(operatorUid)
                        val displaySuffix = content.tipInfo?.tip ?: ""

                        content.recallMessages.map { recall ->
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
                    }

                    16 -> { // SubType16 (may contain GroupReaction or GroupNameChange)
                        val wrapper = GroupGeneral0x2DC(msgContent)
                        val body = wrapper.body
                        val field13 = body.field13

                        when (field13) {
                            35 -> { // GroupReaction
                                val content = body.reaction ?: return listOf()
                                val data = content.data ?: return listOf()
                                val dataMiddle = data.data ?: return listOf()
                                val target = dataMiddle.target ?: return listOf()
                                val dataInner = dataMiddle.data ?: return listOf()

                                val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
                                val msgSeq = target.sequence.toLong()
                                val operatorUid = dataInner.operatorUid
                                val operatorUin = bot.getUinByUid(operatorUid)
                                val faceId = dataInner.code
                                val isAdd = dataInner.type == 1

                                listOf(
                                    GroupMessageReactionEvent(
                                        groupUin = groupUin,
                                        userUin = operatorUin,
                                        userUid = operatorUid,
                                        messageSeq = msgSeq,
                                        faceId = faceId,
                                        isAdd = isAdd
                                    )
                                )
                            }

                            12 -> { // GroupNameChange
                                val eventParam = body.eventParam ?: return listOf()
                                val content = eventParam.pbDecode<GroupNameChange>()
                                val groupUin = if (body.groupUin != 0L) body.groupUin else wrapper.groupUin
                                val newName = content.name
                                val operatorUid = body.operatorUid ?: return listOf()
                                val operatorUin = bot.getUinByUid(operatorUid)

                                listOf(
                                    GroupNameChangeEvent(
                                        groupUin = groupUin,
                                        newGroupName = newName,
                                        operatorUin = operatorUin,
                                        operatorUid = operatorUid
                                    )
                                )
                            }

                            else -> listOf()
                        }
                    }

                    else -> listOf()
                }
            }

            else -> listOf()
        }
    }
}
