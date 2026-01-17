package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetGroupPinReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import kotlin.time.Clock

internal object SetGroupPin : NoOutputOidbService<SetGroupPin.Req>(0x5d6, 1) {
    class Req(
        val groupUin: Long,
        val isPinned: Boolean
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray = SetGroupPinReq {
        it[field1] = 0
        it[field3] = 11
        it[info] = SetGroupPinReq.Info {
            it[groupUin] = payload.groupUin
            it[field400] = SetGroupPinReq.Info.Field400 {
                it[field1] = 13569
                if (payload.isPinned) {
                    it[timestamp] = Buffer().apply {
                        writeInt(Clock.System.now().epochSeconds.toInt())
                    }.readByteArray()
                }
            }
        }
    }.toByteArray()
}
