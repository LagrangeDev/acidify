package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.ProfileLikeReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SendProfileLike : NoOutputOidbService<SendProfileLike.Req>(0x7e5, 104) {
    class Req(
        val targetUid: String,
        val count: Int
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        ProfileLikeReq {
            it[targetUid] = payload.targetUid
            it[field2] = 71
            it[field3] = payload.count
        }.toByteArray()
}