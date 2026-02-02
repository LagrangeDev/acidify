package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.Oidb0x8FCReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetMemberTitle : NoOutputOidbService<SetMemberTitle.Req>(0x8fc, 2) {
    class Req(
        val groupUin: Long,
        val memberUid: String,
        val specialTitle: String
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        Oidb0x8FCReq(
            groupCode = payload.groupUin,
            memLevelInfo = listOf(
                Oidb0x8FCReq.MemberInfo(
                    uid = payload.memberUid,
                    specialTitle = payload.specialTitle.encodeToByteArray(),
                )
            )
        ).pbEncode()
}
