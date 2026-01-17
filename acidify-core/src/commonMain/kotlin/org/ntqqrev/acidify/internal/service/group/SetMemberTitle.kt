package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.Oidb0x8FCReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetMemberTitle : NoOutputOidbService<SetMemberTitle.Req>(0x8fc, 2) {
    class Req(
        val groupUin: Long,
        val memberUid: String,
        val specialTitle: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        Oidb0x8FCReq {
            it[groupCode] = payload.groupUin
            it[memLevelInfo] = listOf(
                Oidb0x8FCReq.MemberInfo {
                    it[uid] = payload.memberUid
                    it[specialTitle] = payload.specialTitle.encodeToByteArray()
                }
            )
        }.toByteArray()
}