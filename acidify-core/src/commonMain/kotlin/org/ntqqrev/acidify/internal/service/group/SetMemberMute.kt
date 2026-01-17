package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetMemberMuteReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetMemberMute : NoOutputOidbService<SetMemberMute.Req>(0x1253, 1) {
    class Req(
        val groupUin: Long,
        val memberUid: String,
        val duration: Int
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        SetMemberMuteReq {
            it[groupCode] = payload.groupUin
            it[type] = 1
            it[body] = SetMemberMuteReq.Body {
                it[targetUid] = payload.memberUid
                it[duration] = payload.duration
            }
        }.toByteArray()
}

