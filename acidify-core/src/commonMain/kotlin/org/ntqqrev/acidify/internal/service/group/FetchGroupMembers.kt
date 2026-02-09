package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupMembersReq
import org.ntqqrev.acidify.internal.proto.oidb.FetchGroupMembersResp
import org.ntqqrev.acidify.internal.service.OidbService
import org.ntqqrev.acidify.internal.util.pbDecode
import org.ntqqrev.acidify.internal.util.pbEncode
import org.ntqqrev.acidify.struct.BotGroupMemberData
import org.ntqqrev.acidify.struct.GroupMemberRole

internal object FetchGroupMembers : OidbService<FetchGroupMembers.Req, FetchGroupMembers.Resp>(0xfe7, 3) {
    internal class Req(
        val groupUin: Long,
        val cookie: ByteArray? = null
    )

    internal class Resp(
        val cookie: ByteArray?,
        val memberDataList: List<BotGroupMemberData>,
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray = FetchGroupMembersReq(
        groupUin = payload.groupUin,
        field2 = 5,
        field3 = 2,
        body = FetchGroupMembersReq.Body(
            memberName = true,
            memberCard = true,
            level = true,
            specialTitle = true,
            joinTimestamp = true,
            lastMsgTimestamp = true,
            shutUpTimestamp = true,
            permission = true,
        ),
        cookie = payload.cookie,
    ).pbEncode()

    override fun parseOidb(client: AbstractClient, payload: ByteArray): Resp {
        val resp = payload.pbDecode<FetchGroupMembersResp>()
        return Resp(
            cookie = resp.cookie,
            memberDataList = resp.members.map {
                val identity = it.id
                BotGroupMemberData(
                    uin = identity.uin,
                    uid = identity.uid,
                    nickname = it.memberName,
                    card = it.memberCard.memberCard ?: "",
                    specialTitle = it.specialTitle ?: "",
                    level = it.level.level,
                    joinedAt = it.joinTimestamp,
                    lastSpokeAt = it.lastMsgTimestamp,
                    mutedUntil = it.shutUpTimestamp,
                    role = GroupMemberRole.from(it.permission)
                )
            }
        )
    }
}
