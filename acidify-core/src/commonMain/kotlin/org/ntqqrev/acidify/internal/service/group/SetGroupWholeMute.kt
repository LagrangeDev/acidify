package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetGroupWholeMuteReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetGroupWholeMute : NoOutputOidbService<SetGroupWholeMute.Req>(0x89a, 0) {
    class Req(
        val groupUin: Long,
        val isMute: Boolean
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray =
        SetGroupWholeMuteReq {
            it[groupCode] = payload.groupUin
            it[state] = SetGroupWholeMuteReq.State {
                it[isMute] = if (payload.isMute) -1 else 0
            }
        }.toByteArray()
}

