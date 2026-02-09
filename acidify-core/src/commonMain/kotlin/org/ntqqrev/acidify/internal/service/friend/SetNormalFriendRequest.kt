package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.SetFriendRequestReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetNormalFriendRequest : NoOutputOidbService<SetNormalFriendRequest.Req>(0xb5d, 44) {
    class Req(
        val targetUid: String,
        val accept: Boolean
    )

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray =
        SetFriendRequestReq(
            accept = if (payload.accept) 3 else 5,
            targetUid = payload.targetUid,
        ).pbEncode()
}
