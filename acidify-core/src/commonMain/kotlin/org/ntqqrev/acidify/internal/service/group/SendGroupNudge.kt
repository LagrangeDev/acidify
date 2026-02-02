package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.PokeReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SendGroupNudge : NoOutputOidbService<SendGroupNudge.Req>(0xed3, 1) {
    class Req(
        val groupUin: Long,
        val targetUin: Long
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        PokeReq(
            targetUin = payload.targetUin,
            groupUin = payload.groupUin,
            friendUin = 0L,
            ext = 0,
        ).pbEncode()
}
