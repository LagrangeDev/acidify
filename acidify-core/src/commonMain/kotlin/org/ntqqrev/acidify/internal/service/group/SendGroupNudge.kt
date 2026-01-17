package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.PokeReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SendGroupNudge : NoOutputOidbService<SendGroupNudge.Req>(0xed3, 1) {
    class Req(
        val groupUin: Long,
        val targetUin: Long
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        PokeReq {
            it[targetUin] = payload.targetUin
            it[groupUin] = payload.groupUin
            it[friendUin] = 0L
            it[ext] = 0
        }.toByteArray()
}

