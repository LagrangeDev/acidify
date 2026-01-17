package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.PokeReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SendFriendNudge : NoOutputOidbService<SendFriendNudge.Req>(0xed3, 1) {
    class Req(
        val friendUin: Long,
        val isSelf: Boolean = false
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        PokeReq {
            it[targetUin] = if (payload.isSelf) client.uin else payload.friendUin
            it[groupUin] = 0
            it[friendUin] = payload.friendUin
            it[ext] = 0
        }.toByteArray()
}