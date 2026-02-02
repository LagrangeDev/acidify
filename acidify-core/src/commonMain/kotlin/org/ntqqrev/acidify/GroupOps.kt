package org.ntqqrev.acidify

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.ntqqrev.acidify.exception.WebApiException
import org.ntqqrev.acidify.internal.json.GroupAnnounceResponse
import org.ntqqrev.acidify.internal.json.GroupAnnounceSendResponse
import org.ntqqrev.acidify.internal.json.GroupEssenceResponse
import org.ntqqrev.acidify.internal.service.group.*
import org.ntqqrev.acidify.internal.util.unescapeHttp
import org.ntqqrev.acidify.message.BotEssenceMessageResult
import org.ntqqrev.acidify.message.internal.MessageParsingContext.Companion.toBotEssenceMessage
import org.ntqqrev.acidify.struct.BotGroupAnnouncement
import org.ntqqrev.acidify.struct.BotGroupNotification

/**
 * 设置群名称
 * @param groupUin 群号
 * @param groupName 新的群名称
 */
suspend fun Bot.setGroupName(
    groupUin: Long,
    groupName: String
) = client.callService(
    SetGroupName,
    SetGroupName.Req(groupUin, groupName)
)

/**
 * 设置群头像
 * @param groupUin 群号
 * @param imageData 图片数据（字节数组）
 */
suspend fun Bot.setGroupAvatar(
    groupUin: Long,
    imageData: ByteArray
) = client.highwayContext.uploadGroupAvatar(groupUin, imageData)

/**
 * 设置群成员的群名片
 * @param groupUin 群号
 * @param memberUin 成员 QQ 号
 * @param card 新的群名片
 */
suspend fun Bot.setGroupMemberCard(
    groupUin: Long,
    memberUin: Long,
    card: String
) = client.callService(
    SetMemberCard,
    SetMemberCard.Req(
        groupUin = groupUin,
        memberUid = getUidByUin(memberUin, groupUin),
        card = card
    )
)

/**
 * 设置群成员的专属头衔
 * @param groupUin 群号
 * @param memberUin 成员 QQ 号
 * @param specialTitle 专属头衔内容，长度不能超过 18 个字节（通常为 6 个汉字或 18 个英文字符）
 */
suspend fun Bot.setGroupMemberSpecialTitle(
    groupUin: Long,
    memberUin: Long,
    specialTitle: String
) = client.callService(
    SetMemberTitle,
    SetMemberTitle.Req(
        groupUin = groupUin,
        memberUid = getUidByUin(memberUin, groupUin),
        specialTitle = specialTitle.takeIf {
            it.encodeToByteArray().size <= 18
        } ?: throw IllegalArgumentException("专属头衔长度不能超过 18 个字节")
    )
)

/**
 * 设置群管理员
 * @param groupUin 群号
 * @param memberUin 成员 QQ 号
 * @param isAdmin 是否设置为管理员，`false` 表示取消管理员
 */
suspend fun Bot.setGroupMemberAdmin(
    groupUin: Long,
    memberUin: Long,
    isAdmin: Boolean
) = client.callService(
    SetMemberAdmin,
    SetMemberAdmin.Req(
        groupUin = groupUin,
        memberUid = getUidByUin(memberUin, groupUin),
        isAdmin = isAdmin
    )
)

/**
 * 设置群成员禁言
 * @param groupUin 群号
 * @param memberUin 成员 QQ 号
 * @param duration 禁言时长（秒），设为 `0` 表示取消禁言
 */
suspend fun Bot.setGroupMemberMute(
    groupUin: Long,
    memberUin: Long,
    duration: Int
) = client.callService(
    SetMemberMute,
    SetMemberMute.Req(
        groupUin = groupUin,
        memberUid = getUidByUin(memberUin, groupUin),
        duration = duration
    )
)

/**
 * 设置群全员禁言
 * @param groupUin 群号
 * @param isMute 是否开启全员禁言，`false` 表示取消全员禁言
 */
suspend fun Bot.setGroupWholeMute(
    groupUin: Long,
    isMute: Boolean
) = client.callService(
    SetGroupWholeMute,
    SetGroupWholeMute.Req(
        groupUin = groupUin,
        isMute = isMute
    )
)

/**
 * 踢出群成员
 * @param groupUin 群号
 * @param memberUin 成员 QQ 号
 * @param rejectAddRequest 是否拒绝再次加群申请
 * @param reason 踢出原因（可选）
 */
suspend fun Bot.kickGroupMember(
    groupUin: Long,
    memberUin: Long,
    rejectAddRequest: Boolean = false,
    reason: String = ""
) = client.callService(
    KickMember,
    KickMember.Req(
        groupUin = groupUin,
        memberUid = getUidByUin(memberUin, groupUin),
        rejectAddRequest = rejectAddRequest,
        reason = reason
    )
)

/**
 * 获取群公告列表
 * @param groupUin 群号
 * @return 群公告列表
 */
suspend fun Bot.getGroupAnnouncements(groupUin: Long): List<BotGroupAnnouncement> {
    val response = httpClient.get("https://web.qun.qq.com/cgi-bin/announce/get_t_list") {
        withBkn()
        parameter("qid", groupUin)
        parameter("ft", 23)
        parameter("ni", 1)
        parameter("n", 1)
        parameter("i", 1)
        parameter("log_read", 1)
        parameter("platform", 1)
        parameter("s", -1)
        parameter("n", 20)
        withCookies("qun.qq.com")
    }

    if (!response.status.isSuccess()) {
        throw WebApiException("获取群公告失败", response.status.value)
    }

    val announceResp = response.body<GroupAnnounceResponse>()
    return (announceResp.feeds + announceResp.inst).map { feed ->
        BotGroupAnnouncement(
            groupUin = groupUin,
            announcementId = feed.noticeId,
            senderId = feed.senderId,
            time = feed.publishTime,
            content = feed.message.text.unescapeHttp(),
            imageUrl = feed.message.images.firstOrNull()?.let {
                "https://gdynamic.qpic.cn/gdynamic/${it.id}/0"
            }
        )
    }
}

/**
 * 发送群公告
 * @param groupUin 群号
 * @param content 公告内容
 * @param imageData 公告图片数据（字节数组，可选，暂不支持）
 * @param showEditCard 是否显示编辑名片提示
 * @param showTipWindow 是否显示提示窗口
 * @param confirmRequired 是否需要确认
 * @param isPinned 是否置顶
 * @return 公告 ID
 */
suspend fun Bot.sendGroupAnnouncement(
    groupUin: Long,
    content: String,
    imageData: ByteArray? = null,
    showEditCard: Boolean = false,
    showTipWindow: Boolean = true,
    confirmRequired: Boolean = true,
    isPinned: Boolean = false,
): String {
    if (imageData != null) {
        TODO("暂不支持带图片的群公告")
    }

    val bkn = getCsrfToken()
    val response = httpClient.post("https://web.qun.qq.com/cgi-bin/announce/add_qun_notice") {
        withBkn()
        withCookies("qun.qq.com")
        userAgent("Dalvik/2.1.0 (Linux; U; Android 7.1.2; PCRT00 Build/N2G48H)")
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(
            FormDataContent(
                Parameters.build {
                    append("qid", groupUin.toString())
                    append("bkn", bkn.toString())
                    append("text", content)
                    append("pinned", if (isPinned) "1" else "0")
                    append("type", "1")
                    append(
                        "settings",
                        Json.encodeToString(
                            buildJsonObject {
                                put("is_show_edit_card", if (showEditCard) 1 else 0)
                                put("tip_window_type", if (showTipWindow) 1 else 0)
                                put("confirm_required", if (confirmRequired) 1 else 0)
                            }
                        )
                    )
                }
            )
        )
    }

    if (!response.status.isSuccess()) {
        throw WebApiException("发送群公告失败", response.status.value)
    }

    val sendResp = response.body<GroupAnnounceSendResponse>()
    return sendResp.noticeId
}

/**
 * 删除群公告
 * @param groupUin 群号
 * @param announcementId 公告 ID
 */
suspend fun Bot.deleteGroupAnnouncement(
    groupUin: Long,
    announcementId: String
) {
    val response = httpClient.get("https://web.qun.qq.com/cgi-bin/announce/del_feed") {
        withBkn()
        parameter("fid", announcementId)
        parameter("qid", groupUin)
        parameter("ft", 23)
        parameter("op", 1)
        withCookies("qun.qq.com")
    }

    if (!response.status.isSuccess()) {
        throw WebApiException("删除群公告失败", response.status.value)
    }
}

/**
 * 获取群精华消息列表
 * @param groupUin 群号
 * @param pageIndex 页码索引
 * @param pageSize 每页包含的精华消息数量
 * @return 精华消息列表
 */
suspend fun Bot.getGroupEssenceMessages(
    groupUin: Long,
    pageIndex: Int,
    pageSize: Int
): BotEssenceMessageResult {
    val response = httpClient.get("https://qun.qq.com/cgi-bin/group_digest/digest_list") {
        withBkn()
        parameter("random", 7800)
        parameter("X-CROSS-ORIGIN", "fetch")
        parameter("group_code", groupUin)
        parameter("page_start", pageIndex)
        parameter("page_limit", pageSize)
        withCookies("qun.qq.com")
    }

    if (!response.status.isSuccess()) {
        throw WebApiException("获取群精华消息失败", response.status.value)
    }

    val essenceResp = response.body<GroupEssenceResponse>()
    val msgList = essenceResp.data.msgList ?: emptyList()

    return BotEssenceMessageResult(
        messages = msgList.mapNotNull { it.toBotEssenceMessage(groupUin) },
        isEnd = essenceResp.data.isEnd
    )
}

/**
 * 设置群精华消息
 * @param groupUin 群号
 * @param sequence 消息序列号
 * @param isSet 是否设置为精华消息，`false` 表示取消精华
 */
suspend fun Bot.setGroupEssenceMessage(
    groupUin: Long,
    sequence: Long,
    isSet: Boolean
) {
    val random = getGroupHistoryMessages(groupUin, 1, sequence)
        .messages
        .firstOrNull()
        ?.random
        ?: throw IllegalStateException("消息不存在，无法获取 random 字段")
    client.callService(
        if (isSet) SetGroupEssenceMessage.Set else SetGroupEssenceMessage.Unset,
        SetGroupEssenceMessage.Req(groupUin, sequence, random)
    )
}

/**
 * 退出群聊
 * @param groupUin 群号
 */
suspend fun Bot.quitGroup(groupUin: Long) = client.callService(
    QuitGroup,
    QuitGroup.Req(groupUin)
)

/**
 * 发送群消息表情回应
 * @param groupUin 群号
 * @param sequence 消息序列号
 * @param code 表情代码
 * @param isAdd 是否添加表情回应，`false` 表示取消回应
 */
suspend fun Bot.setGroupMessageReaction(
    groupUin: Long,
    sequence: Long,
    code: String,
    isAdd: Boolean = true
) = client.callService(
    if (isAdd) SetGroupMessageReaction.Add else SetGroupMessageReaction.Remove,
    SetGroupMessageReaction.Req(
        groupUin = groupUin,
        sequence = sequence,
        code = code,
        type = if (code in faceDetailMap) 1 else 2
    )
)

/**
 * 发送群戳一戳
 * @param groupUin 群号
 * @param targetUin 被戳的成员 QQ 号
 */
suspend fun Bot.sendGroupNudge(
    groupUin: Long,
    targetUin: Long
) = client.callService(
    SendGroupNudge,
    SendGroupNudge.Req(groupUin, targetUin)
)

/**
 * 获取群通知列表
 * @param startSequence 起始通知序列号，为 null 则从最新通知开始获取
 * @param isFiltered 是否只获取被过滤的通知（风险账号发起）
 * @param count 获取的最大通知数量
 * @return 群通知列表和下一页起始序列号
 */
suspend fun Bot.getGroupNotifications(
    startSequence: Long? = null,
    isFiltered: Boolean = false,
    count: Int = 20
): Pair<List<BotGroupNotification>, Long?> {
    val resp = client.callService(
        if (isFiltered) FetchGroupNotifications.Filtered else FetchGroupNotifications.Normal,
        FetchGroupNotifications.Req(startSequence ?: 0, count)
    )
    val notifications = resp.notifications.mapNotNull {
        with(BotGroupNotification) { parseNotification(it, isFiltered) }
    }
    return notifications to resp.nextSequence.takeIf { it != 0L }
}

/**
 * 处理群请求（同意/拒绝）
 * @param groupUin 群号
 * @param sequence 通知序列号
 * @param eventType 事件类型（1=入群请求, 22=邀请他人入群）
 * @param accept 是否同意（true=同意, false=拒绝）
 * @param isFiltered 是否是被过滤的请求
 * @param reason 拒绝理由（仅在拒绝时使用）
 */
suspend fun Bot.setGroupRequest(
    groupUin: Long,
    sequence: Long,
    eventType: Int,
    accept: Boolean,
    isFiltered: Boolean = false,
    reason: String = ""
) {
    client.callService(
        if (isFiltered) SetGroupRequest.Filtered else SetGroupRequest.Normal,
        SetGroupRequest.Req(
            groupUin = groupUin,
            sequence = sequence,
            eventType = eventType,
            accept = if (accept) 1 else 2,
            reason = reason
        )
    )
}

/**
 * 处理群邀请（他人邀请自己入群）
 * @param groupUin 群号
 * @param invitationSeq 邀请序列号
 * @param accept 是否同意
 */
suspend fun Bot.setGroupInvitation(
    groupUin: Long,
    invitationSeq: Long,
    accept: Boolean
) {
    client.callService(
        SetGroupRequest.Normal,
        SetGroupRequest.Req(
            groupUin = groupUin,
            sequence = invitationSeq,
            eventType = 2,
            accept = if (accept) 1 else 2,
            reason = ""
        )
    )
}
