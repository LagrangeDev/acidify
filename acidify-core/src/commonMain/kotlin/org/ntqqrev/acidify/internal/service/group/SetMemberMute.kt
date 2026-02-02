package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.SetMemberMuteReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetMemberMute : NoOutputOidbService<SetMemberMute.Req>(0x1253, 1) {
    class Req(
        val groupUin: Long,
        val memberUid: String,
        val duration: Int
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        SetMemberMuteReq(
            groupCode = payload.groupUin,
            type = 1,
            body = SetMemberMuteReq.Body(
                targetUid = payload.memberUid,
                duration = payload.duration,
            )
        ).pbEncode()
}
