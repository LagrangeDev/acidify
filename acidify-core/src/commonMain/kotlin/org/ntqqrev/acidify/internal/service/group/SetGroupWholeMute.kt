package org.ntqqrev.acidify.internal.service.group

import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.ntqqrev.acidify.internal.AbstractClient
import org.ntqqrev.acidify.internal.proto.oidb.SetGroupWholeMuteReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetGroupWholeMute : NoOutputOidbService<SetGroupWholeMute.Req>(0x89a, 0) {
    class Req(
        val groupUin: Long,
        val isMute: Boolean
    )

    private val protobufModule = ProtoBuf {
        encodeDefaults = true
    }

    override fun buildOidb(client: AbstractClient, payload: Req): ByteArray = protobufModule.encodeToByteArray(
        SetGroupWholeMuteReq(
            groupCode = payload.groupUin,
            state = SetGroupWholeMuteReq.State(
                isMute = if (payload.isMute) -1 else 0
            )
        )
    )
}
