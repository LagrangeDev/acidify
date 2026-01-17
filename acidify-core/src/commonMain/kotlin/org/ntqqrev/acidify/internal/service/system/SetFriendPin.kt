package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.oidb.SetFriendPinReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import kotlin.time.Clock

internal object SetFriendPin : NoOutputOidbService<SetFriendPin.Req>(0x5d6, 18) {
    class Req(
        val friendUid: String,
        val isPinned: Boolean
    )

    override fun buildOidb(client: IClient, payload: Req): ByteArray = SetFriendPinReq {
        it[field1] = 0
        it[field3] = 1
        it[info] = SetFriendPinReq.Info {
            it[friendUid] = payload.friendUid
            it[field400] = SetFriendPinReq.Info.Field400 {
                it[field1] = 13578
                if (payload.isPinned) {
                    it[timestamp] = Buffer().apply {
                        writeInt(Clock.System.now().epochSeconds.toInt())
                    }.readByteArray()
                }
            }
        }
    }.toByteArray()
}
