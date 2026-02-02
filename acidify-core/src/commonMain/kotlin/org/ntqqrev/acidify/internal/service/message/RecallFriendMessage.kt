package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.proto.message.action.C2CRecallMsg
import org.ntqqrev.acidify.internal.service.NoOutputService
import org.ntqqrev.acidify.internal.util.pbEncode

internal object RecallFriendMessage :
    NoOutputService<RecallFriendMessage.Req>("trpc.msg.msg_svc.MsgService.SsoC2CRecallMsg") {
    class Req(
        val targetUid: String,
        val clientSequence: Long,
        val messageSequence: Long,
        val random: Int,
        val timestamp: Long
    )

    override fun build(client: LagrangeClient, payload: Req): ByteArray {
        return C2CRecallMsg(
            type = 1,
            targetUid = payload.targetUid,
            info = C2CRecallMsg.Info(
                clientSequence = payload.clientSequence,
                random = payload.random,
                messageId = (0x01000000L shl 32) or payload.random.toLong(),
                timestamp = payload.timestamp,
                field5 = 0,
                messageSequence = payload.messageSequence,
            ),
            settings = C2CRecallMsg.Settings(
                field1 = false,
                field2 = false,
            ),
            field6 = false,
        ).pbEncode()
    }
}
