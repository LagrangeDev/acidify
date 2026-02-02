package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.KickMemberReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object KickMember : NoOutputOidbService<KickMember.Req>(0x8a0, 1) {
    class Req(
        val groupUin: Long,
        val memberUid: String,
        val rejectAddRequest: Boolean,
        val reason: String
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        KickMemberReq(
            groupCode = payload.groupUin,
            kickFlag = 0,
            targetUid = payload.memberUid,
            rejectAddRequest = payload.rejectAddRequest,
            reason = payload.reason,
        ).pbEncode()
}
