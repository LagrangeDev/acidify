package org.ntqqrev.acidify.internal.service.group

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.SetGroupWholeMuteReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object SetGroupWholeMute : NoOutputOidbService<SetGroupWholeMute.Req>(0x89a, 0) {
    class Req(
        val groupUin: Long,
        val isMute: Boolean
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray =
        SetGroupWholeMuteReq(
            groupCode = payload.groupUin,
            state = SetGroupWholeMuteReq.State(
                isMute = if (payload.isMute) -1 else 0
            )
        ).pbEncode()
}
