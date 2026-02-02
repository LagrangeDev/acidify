package org.ntqqrev.acidify.internal.service.system

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.oidb.SetFriendPinReq
import org.ntqqrev.acidify.internal.service.NoOutputOidbService
import org.ntqqrev.acidify.internal.util.pbEncode
import kotlin.time.Clock

internal object SetFriendPin : NoOutputOidbService<SetFriendPin.Req>(0x5d6, 18) {
    class Req(
        val friendUid: String,
        val isPinned: Boolean
    )

    override fun buildOidb(client: LagrangeClient, payload: Req): ByteArray = SetFriendPinReq(
        field1 = 0,
        field3 = 1,
        info = SetFriendPinReq.Info(
            friendUid = payload.friendUid,
            field400 = SetFriendPinReq.Info.Field400(
                field1 = 13578,
                timestamp = if (payload.isPinned) {
                    Buffer().apply { writeInt(Clock.System.now().epochSeconds.toInt()) }.readByteArray()
                } else {
                    byteArrayOf()
                }
            ),
        ),
    ).pbEncode()
}
