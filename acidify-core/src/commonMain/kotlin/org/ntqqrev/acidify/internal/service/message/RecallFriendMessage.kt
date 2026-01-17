package org.ntqqrev.acidify.internal.service.message

import org.ntqqrev.acidify.internal.IClient
import org.ntqqrev.acidify.internal.packet.message.action.C2CRecallMsg
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputService

internal object RecallFriendMessage :
    NoOutputService<RecallFriendMessage.Req>("trpc.msg.msg_svc.MsgService.SsoC2CRecallMsg") {
    class Req(
        val targetUid: String,
        val clientSequence: Long,
        val messageSequence: Long,
        val random: Int,
        val timestamp: Long
    )

    override fun build(client: IClient, payload: Req): ByteArray {
        return C2CRecallMsg {
            it[type] = 1
            it[targetUid] = payload.targetUid
            it[info] = C2CRecallMsg.Info {
                it[clientSequence] = payload.clientSequence
                it[random] = payload.random
                it[messageId] = (0x01000000L shl 32) or payload.random.toLong()
                it[timestamp] = payload.timestamp
                it[field5] = 0
                it[messageSequence] = payload.messageSequence
            }
            it[settings] = C2CRecallMsg.Settings {
                it[field1] = false
                it[field2] = false
            }
            it[field6] = false
        }.toByteArray()
    }
}