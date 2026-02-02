package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.DeleteFriendReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object DeleteFriend : NoOutputOidbService<DeleteFriend.Req>(0x126b, 0) {
    class Req(
        val friendUid: String,
        val block: Boolean,
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray = DeleteFriendReq(
        body = DeleteFriendReq.Body(
            targetUid = payload.friendUid,
            field2 = DeleteFriendReq.Body.Field2(
                field1 = 130,
                field2 = 109,
                field3 = DeleteFriendReq.Body.Field2.Field3(
                    field1 = 8,
                    field2 = 8,
                    field3 = 50,
                )
            ),
            block = payload.block,
        )
    ).pbEncode()
}
