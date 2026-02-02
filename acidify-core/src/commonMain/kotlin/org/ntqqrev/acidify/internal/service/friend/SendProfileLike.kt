package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.ProfileLikeReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SendProfileLike : NoOutputOidbService<SendProfileLike.Req>(0x7e5, 104) {
    class Req(
        val targetUid: String,
        val count: Int
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        ProfileLikeReq(
            targetUid = payload.targetUid,
            field2 = 71,
            field3 = payload.count,
        ).pbEncode()
}
