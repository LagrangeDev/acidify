package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetFriendRequestReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetNormalFriendRequest : NoOutputOidbService<SetNormalFriendRequest.Req>(0xb5d, 44) {
    class Req(
        val targetUid: String,
        val accept: Boolean
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        SetFriendRequestReq {
            it[accept] = if (payload.accept) 3 else 5
            it[targetUid] = payload.targetUid
        }.toByteArray()
}
