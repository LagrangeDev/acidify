package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.QuitGroupReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object QuitGroup : NoOutputOidbService<QuitGroup.Req>(0x1097, 1) {
    class Req(
        val groupUin: Long
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        QuitGroupReq {
            it[groupCode] = payload.groupUin
        }.toByteArray()
}

