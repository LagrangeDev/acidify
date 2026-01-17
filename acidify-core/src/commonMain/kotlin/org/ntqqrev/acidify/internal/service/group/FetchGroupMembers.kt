package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupMembersReq
import org.ntqqrev.acidify.internal.packet.oidb.FetchGroupMembersResp
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.OidbService
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

    override fun buildOidb(client: IClient, payload: Req): ByteArray = FetchGroupMembersReq {
        it[groupUin] = payload.groupUin
        it[field2] = 5
        it[field3] = 2
        it[body] = FetchGroupMembersReq.Body {
            it[memberName] = true
            it[memberCard] = true
            it[level] = true
            it[specialTitle] = true
            it[joinTimestamp] = true
            it[lastMsgTimestamp] = true
            it[shutUpTimestamp] = true
            it[permission] = true
        }
        it[cookie] = payload.cookie
    }.toByteArray()

    override fun parseOidb(client: IClient, payload: ByteArray): Resp {
        val resp = FetchGroupMembersResp(payload)
        return Resp(
            cookie = resp.get { cookie },
            memberDataList = resp.get { members }.map {
                val identity = it.get { id }
                BotGroupMemberData(
                    uin = identity.get { uin },
                    uid = identity.get { uid },
                    nickname = it.get { memberName },
                    card = it.get { memberCard }.get { memberCard } ?: "",
                    specialTitle = it.get { specialTitle } ?: "",
                    level = it.get { level }.get { level },
                    joinedAt = it.get { joinTimestamp },
                    lastSpokeAt = it.get { lastMsgTimestamp },
                    mutedUntil = it.get { shutUpTimestamp },
                    role = GroupMemberRole.from(it.get { permission })
                )
            }
        )
    }
}