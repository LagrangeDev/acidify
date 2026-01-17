package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetFilteredFriendRequestReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetFilteredFriendRequest : NoOutputOidbService<String>(0xd72, 0) {
    override fun buildOidb(client: IClient, payload: String): ByteArray =
        SetFilteredFriendRequestReq {
            it[selfUid] = client.uid
            it[requestUid] = payload
        }.toByteArray()
}
