package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetGroupNameReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetGroupName : NoOutputOidbService<SetGroupName.Req>(0x89a, 15) {
    class Req(
        val groupUin: Long,
        val groupName: String
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        SetGroupNameReq {
            it[groupCode] = payload.groupUin
            it[targetName] = payload.groupName
        }.toByteArray()
}

