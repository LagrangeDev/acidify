package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.SetGroupPinReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode
import kotlin.time.Clock

internal object SetGroupPin : NoOutputOidbService<SetGroupPin.Req>(0x5d6, 1) {
    class Req(
        val groupUin: Long,
        val isPinned: Boolean
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray = SetGroupPinReq(
        field1 = 0,
        field3 = 11,
        info = SetGroupPinReq.Info(
            groupUin = payload.groupUin,
            field400 = SetGroupPinReq.Info.Field400(
                field1 = 13569,
                timestamp = if (payload.isPinned) {
                    Buffer().apply { writeInt(Clock.System.now().epochSeconds.toInt()) }.readByteArray()
                } else {
                    byteArrayOf()
                }
            ),
        ),
    ).pbEncode()
}
