package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.SetFilteredFriendRequestReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetFilteredFriendRequest : NoOutputOidbService<String>(0xd72, 0) {
    override fun buildOidb(client: LagrangeClient, payload: String): ByteArray =
        SetFilteredFriendRequestReq(
            selfUid = client.sessionStore.uid,
            requestUid = payload,
        ).pbEncode()
}
