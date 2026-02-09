package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.PokeReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SendFriendNudge : NoOutputOidbService<SendFriendNudge.Req>(0xed3, 1) {
    class Req(
        val friendUin: Long,
        val isSelf: Boolean = false
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        PokeReq(
            targetUin = if (payload.isSelf) client.uin else payload.friendUin,
            groupUin = 0,
            friendUin = payload.friendUin,
            ext = 0,
        ).pbEncode()
}
