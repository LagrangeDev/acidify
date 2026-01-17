package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.DeleteFriendReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object DeleteFriend : NoOutputOidbService<DeleteFriend.Req>(0x126b, 0) {
    class Req(
        val friendUid: String,
        val block: Boolean,
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray = DeleteFriendReq {
        it[body] = DeleteFriendReq.Body {
            it[targetUid] = payload.friendUid
            it[field2] = DeleteFriendReq.Body.Field2 {
                it[field1] = 130
                it[field2] = 109
                it[field3] = DeleteFriendReq.Body.Field2.Field3 {
                    it[field1] = 8
                    it[field2] = 8
                    it[field3] = 50
                }
            }
            it[block] = payload.block
        }
    }.toByteArray()
}